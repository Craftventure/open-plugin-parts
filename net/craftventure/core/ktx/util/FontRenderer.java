package net.craftventure.core.ktx.util;

import java.awt.image.BufferedImage;

public class FontRenderer {
    public float[] readFontTexture(BufferedImage bufferedImage) {
        float[] charWidth = new float[256];
        int imgWidth = bufferedImage.getWidth();
        int imgHeight = bufferedImage.getHeight();
        int charW = imgWidth / 16;
        int charH = imgHeight / 16;
        int[] ai = new int[imgWidth * imgHeight];
        float kx = (float) imgWidth / 128.0F;
        bufferedImage.getRGB(0, 0, imgWidth, imgHeight, ai, 0, imgWidth);
        int k = 0;

        while (k < 256) {
            int cx = k % 16;
            int cy = k / 16;
            boolean px = false;
            int var19 = charW - 1;

            while (true) {
                if (var19 >= 0) {
                    int x = cx * charW + var19;
                    boolean flag = true;

                    for (int py = 0; py < charH && flag; ++py) {
                        int ypos = (cy * charH + py) * imgWidth;
                        int col = ai[x + ypos];
                        int al = col >> 24 & 255;

                        if (al > 16) {
                            flag = false;
                        }
                    }

                    if (flag) {
                        --var19;
                        continue;
                    }
                }

                if (k == 65) {
                    k = k;
                }

                if (k == 32) {
                    if (charW <= 8) {
                        var19 = (int) (2.0F * kx);
                    } else {
                        var19 = (int) (1.5F * kx);
                    }
                }

                charWidth[k] = (float) (var19 + 1) / kx + 1.0F;
                ++k;
                break;
            }
        }

        return charWidth;
    }
}
