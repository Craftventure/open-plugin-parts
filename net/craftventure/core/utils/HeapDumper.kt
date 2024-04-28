package net.craftventure.core.utils

import com.sun.management.HotSpotDiagnosticMXBean

import java.lang.management.ManagementFactory

object HeapDumper {
    // This is the name of the HotSpot Diagnostic MBean
    private val HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic"

    // field to store the hotspot diagnostic MBean
    @Volatile
    private var hotspotMBean: HotSpotDiagnosticMXBean? = null

    fun dumpHeap(fileName: String, live: Boolean) {
        // initialize hotspot diagnostic MBean
        initHotspotMBean()
        try {
            hotspotMBean!!.dumpHeap(fileName, live)
        } catch (re: RuntimeException) {
            throw re
        } catch (exp: Exception) {
            throw RuntimeException(exp)
        }

    }

    // initialize the hotspot diagnostic MBean field
    private fun initHotspotMBean() {
        if (hotspotMBean == null) {
            synchronized(HeapDumper::class.java) {
                if (hotspotMBean == null) {
                    hotspotMBean = getHotspotMBean()
                }
            }
        }
    }

    // get the hotspot diagnostic MBean from the
    // platform MBean server
    private fun getHotspotMBean(): HotSpotDiagnosticMXBean {
        try {
            val server = ManagementFactory.getPlatformMBeanServer()
            return ManagementFactory.newPlatformMXBeanProxy(
                server,
                HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean::class.java
            )
        } catch (re: RuntimeException) {
            throw re
        } catch (exp: Exception) {
            throw RuntimeException(exp)
        }

    }
}
