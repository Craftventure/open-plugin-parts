package net.craftventure.core.ride.trackedride.segment;

import net.craftventure.bukkit.ktx.extension.VectorExtensionsKt;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.ride.trackedride.*;
import net.craftventure.core.ride.trackedride.trackpiece.BezierSplineTrackPiece;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * The most basic track segment consisting out of a bezier spline
 */
public class SplinedTrackSegment extends TrackSegment {

    private final List<BezierSplineTrackPiece> trackPieceList = new ArrayList<>();

    public SplinedTrackSegment(String id, TrackedRide trackedRide) {
        this(id, id, trackedRide);
    }

    public SplinedTrackSegment(String id, String displayName, TrackedRide trackedRide) {
        super(id, displayName, trackedRide);
    }

    public void add(Vector offset, SplineNode... nodes) {
        add(offset, false, nodes);
    }

    public void add(Vector offset, Boolean precisionFix, SplineNode... nodes) {
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = new SplineNode(nodes[i]);
                SplineNode node = nodes[i];
                node.getIn().addOffset(offset.getX(), offset.getY(), offset.getZ());
                node.getKnot().addOffset(offset.getX(), offset.getY(), offset.getZ());
                node.getOut().addOffset(offset.getX(), offset.getY(), offset.getZ());
            }
            for (int i = 1; i < nodes.length; i++) {
                trackPieceList.add(new BezierSplineTrackPiece(nodes[i - 1], nodes[i], precisionFix));
            }
        }
    }

    public void add(SplineNode... nodes) {
        this.add(new Vector(), nodes);
    }

    @Override
    public double getLength() {
        if (length == -1) {
            length = 0;
            for (int i = 0; i < trackPieceList.size(); i++) {
                BezierSplineTrackPiece trackPiece = trackPieceList.get(i);
                length += trackPiece.getLength();
            }
            double currentT = 0;
            for (int i = 0; i < trackPieceList.size(); i++) {
                BezierSplineTrackPiece trackPiece = trackPieceList.get(i);
                trackPiece.setStartT(currentT);
                currentT += trackPiece.getLength() / length;
                trackPiece.setEndT(currentT);
            }
        }
        return length;
    }

    @Override
    public double getBanking(double distance, boolean applyInterceptors) {
        double currentDistance = 0;
        for (int i = 0; i < trackPieceList.size(); i++) {
            BezierSplineTrackPiece trackPiece = trackPieceList.get(i);
            if (currentDistance <= distance && distance <= currentDistance + trackPiece.getLength()) {
                double t = (distance - currentDistance) / trackPiece.getLength();
                double bankingStart = trackPiece.getA().getBanking();
                double bankingEnd = trackPiece.getB().getBanking();
                double banking = (1f - t) * bankingStart + t * bankingEnd;
                if (applyInterceptors && bankingInterceptor != null) {
                    banking = bankingInterceptor.getBanking(this, distance, banking);
                }
                return banking;
            }
            currentDistance += trackPiece.getLength();
        }
        if (applyInterceptors && bankingInterceptor != null) {
            return bankingInterceptor.getBanking(this, distance, 0);
        }
        return 0;
    }

    @Override
    public void getStartPosition(Vector position) {
        VectorExtensionsKt.set(position, trackPieceList.get(0).getA().getKnot().toVector());
    }

    @Override
    public void getEndPosition(Vector position) {
        VectorExtensionsKt.set(position, trackPieceList.get(trackPieceList.size() - 1).getA().getKnot().toVector());
    }

    @Override
    public void getPosition(double distance, Vector position, boolean applyInterceptors) {
        double currentDistance = 0;
        for (int i = 0; i < trackPieceList.size(); i++) {
            BezierSplineTrackPiece trackPiece = trackPieceList.get(i);
            if (currentDistance <= distance && distance <= currentDistance + trackPiece.getLength()) {
                double t = distance == currentDistance ? 1 : (distance - currentDistance) / trackPiece.getLength();
                SplineHelper.getValue(trackPiece.translateT(t),
                        trackPiece.getA().getKnot().toVector(), trackPiece.getA().getOut().toVector(),
                        trackPiece.getB().getIn().toVector(), trackPiece.getB().getKnot().toVector(),
                        position);

                if (applyInterceptors && positionInterceptor != null) {
                    positionInterceptor.getPosition(this, distance, position);
                }
                return;
            }
            currentDistance += trackPiece.getLength();
        }
        Logger.warn("Failed to find position for distance " + distance);
    }

    @Override
    public void applyForces(RideCar car, double distanceSinceLastUpdate) {
        double acceleration = (9.8 * Math.cos(Math.PI - car.pitchRadian) / 20.0) * this.gravitationalInfluence;
        car.setAcceleration(acceleration);
        car.setVelocity(car.getVelocity() * friction);
    }

    public TrackSegmentJson toJson() {
        SplinedTrackSegmentJson json = new SplinedTrackSegmentJson();
        return toJson(json);
    }
}
