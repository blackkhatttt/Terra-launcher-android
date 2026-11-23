package com.universe.solarcrystal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small immutable scene index for CPU-based object picking.
 * This is a reusable selection layer, not a renderer or a RubyDung world adapter.
 */
public final class RubyDungPickingScene {
    private final List<Target> targets;

    public RubyDungPickingScene() {
        this.targets = Collections.emptyList();
    }

    private RubyDungPickingScene(List<Target> targets) {
        this.targets = Collections.unmodifiableList(new ArrayList<Target>(targets));
    }

    public static final class Target {
        public final String id;
        public final RubyDungRayPicker.Aabb bounds;

        public Target(String id, RubyDungRayPicker.Aabb bounds) {
            if (id == null || id.trim().length() == 0)
                throw new IllegalArgumentException("Target id must not be empty");
            if (bounds == null)
                throw new IllegalArgumentException("Target bounds must not be null");
            this.id = id;
            this.bounds = bounds;
        }
    }

    public static final class Selection {
        public final String id;
        public final double distance;
        public final RubyDungRayPicker.Vec3 point;

        private Selection(String id, double distance, RubyDungRayPicker.Vec3 point) {
            this.id = id;
            this.distance = distance;
            this.point = point;
        }
    }

    public int size() {
        return targets.size();
    }

    public RubyDungPickingScene withTarget(Target target) {
        if (target == null) throw new IllegalArgumentException("Target must not be null");
        for (Target current : targets) {
            if (current.id.equals(target.id))
                throw new IllegalArgumentException("Duplicate target id: " + target.id);
        }
        List<Target> copy = new ArrayList<Target>(targets);
        copy.add(target);
        return new RubyDungPickingScene(copy);
    }

    public RubyDungPickingScene withoutTarget(String id) {
        if (id == null) return this;
        List<Target> copy = new ArrayList<Target>(targets);
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).id.equals(id)) {
                copy.remove(i);
                return new RubyDungPickingScene(copy);
            }
        }
        return this;
    }

    /** Returns the nearest target hit by the ray, or null when nothing is selected. */
    public Selection pick(RubyDungRayPicker.Vec3 origin,
                          RubyDungRayPicker.Vec3 direction,
                          double maxDistance) {
        if (targets.isEmpty()) return null;
        RubyDungRayPicker.Aabb[] boxes = new RubyDungRayPicker.Aabb[targets.size()];
        for (int i = 0; i < targets.size(); i++) boxes[i] = targets.get(i).bounds;
        RubyDungRayPicker.Hit hit =
            RubyDungRayPicker.nearest(origin, direction, boxes, maxDistance);
        if (hit == null) return null;
        Target target = targets.get(hit.objectIndex);
        return new Selection(target.id, hit.distance, hit.point);
    }

    public static String selfTest() {
        RubyDungPickingScene scene = new RubyDungPickingScene()
            .withTarget(new Target("far-rock", new RubyDungRayPicker.Aabb(
                new RubyDungRayPicker.Vec3(7, -1, -1),
                new RubyDungRayPicker.Vec3(8, 1, 1))))
            .withTarget(new Target("near-crate", new RubyDungRayPicker.Aabb(
                new RubyDungRayPicker.Vec3(3, -1, -1),
                new RubyDungRayPicker.Vec3(4, 1, 1))));
        Selection selected = scene.pick(new RubyDungRayPicker.Vec3(0, 0, 0),
            new RubyDungRayPicker.Vec3(1, 0, 0), 20);
        if (selected == null || !"near-crate".equals(selected.id)
                || Math.abs(selected.distance - 3.0) > 1e-9)
            return "FAIL: nearest target identity";
        if (scene.withoutTarget("near-crate").size() != 1)
            return "FAIL: immutable target removal";
        if (scene.size() != 2)
            return "FAIL: original scene changed";
        if (scene.pick(new RubyDungRayPicker.Vec3(0, 5, 0),
                new RubyDungRayPicker.Vec3(1, 0, 0), 20) != null)
            return "FAIL: miss returns no selection";
        return "PASS: stable target identity, nearest selection, immutable updates and miss handling.";
    }
}
