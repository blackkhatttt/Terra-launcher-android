package com.universe.solarcrystal;

/**
 * CPU ray-versus-AABB picking primitive for replacing legacy OpenGL selection mode.
 * Pure Java math module; it is not yet connected to RubyDung's world renderer.
 */
public final class RubyDungRayPicker {
    private RubyDungRayPicker() {}

    public static final class Vec3 {
        public final double x, y, z;
        public Vec3(double x, double y, double z) {
            if (!finite(x) || !finite(y) || !finite(z))
                throw new IllegalArgumentException("Vector components must be finite");
            this.x = x; this.y = y; this.z = z;
        }
        public Vec3 addScaled(Vec3 direction, double t) {
            return new Vec3(x + direction.x * t, y + direction.y * t, z + direction.z * t);
        }
    }

    public static final class Aabb {
        public final Vec3 min, max;
        public Aabb(Vec3 min, Vec3 max) {
            if (min == null || max == null
                    || min.x > max.x || min.y > max.y || min.z > max.z)
                throw new IllegalArgumentException("Invalid AABB bounds");
            this.min = min; this.max = max;
        }
    }

    public static final class Hit {
        public final int objectIndex;
        public final double distance;
        public final Vec3 point;
        private Hit(int objectIndex, double distance, Vec3 point) {
            this.objectIndex = objectIndex; this.distance = distance; this.point = point;
        }
    }

    /** Returns the nearest non-negative ray parameter, or null when there is no hit. */
    public static Hit intersect(Vec3 origin, Vec3 direction, Aabb box,
                                int objectIndex, double maxDistance) {
        if (origin == null || direction == null || box == null
                || !finite(maxDistance) || maxDistance < 0.0)
            return null;
        if (direction.x == 0.0 && direction.y == 0.0 && direction.z == 0.0)
            return null;

        double near = 0.0;
        double far = maxDistance;
        double[] o = {origin.x, origin.y, origin.z};
        double[] d = {direction.x, direction.y, direction.z};
        double[] lo = {box.min.x, box.min.y, box.min.z};
        double[] hi = {box.max.x, box.max.y, box.max.z};

        for (int axis = 0; axis < 3; axis++) {
            if (d[axis] == 0.0) {
                if (o[axis] < lo[axis] || o[axis] > hi[axis]) return null;
                continue;
            }
            double a = (lo[axis] - o[axis]) / d[axis];
            double b = (hi[axis] - o[axis]) / d[axis];
            if (a > b) { double swap = a; a = b; b = swap; }
            near = Math.max(near, a);
            far = Math.min(far, b);
            if (near > far) return null;
        }
        if (far < 0.0 || near > maxDistance) return null;
        return new Hit(objectIndex, near, origin.addScaled(direction, near));
    }

    /** Tests boxes in input order and returns the closest hit; ties keep the first box. */
    public static Hit nearest(Vec3 origin, Vec3 direction, Aabb[] boxes, double maxDistance) {
        if (boxes == null || !finite(maxDistance) || maxDistance < 0.0) return null;
        Hit best = null;
        for (int i = 0; i < boxes.length; i++) {
            Hit hit = intersect(origin, direction, boxes[i], i,
                    best == null ? maxDistance : best.distance);
            if (hit != null && (best == null || hit.distance < best.distance)) best = hit;
        }
        return best;
    }

    /** Small deterministic smoke test also used by the launcher's diagnostic button. */
    public static String selfTest() {
        Vec3 origin = new Vec3(0, 0, 0);
        Vec3 direction = new Vec3(1, 0, 0);
        Aabb near = new Aabb(new Vec3(3, -1, -1), new Vec3(4, 1, 1));
        Aabb far = new Aabb(new Vec3(7, -1, -1), new Vec3(8, 1, 1));
        Hit hit = nearest(origin, direction, new Aabb[] {far, near}, 20);
        if (hit == null || hit.objectIndex != 1 || Math.abs(hit.distance - 3.0) > 1e-9)
            return "FAIL: nearest box selection";
        if (intersect(origin, new Vec3(0, 1, 0), near, 0, 20) != null)
            return "FAIL: parallel-axis miss";
        if (intersect(origin, new Vec3(-1, 0, 0), near, 0, 20) != null)
            return "FAIL: box behind ray";
        Hit inside = intersect(new Vec3(3.5, 0, 0), direction, near, 2, 20);
        if (inside == null || inside.distance != 0.0)
            return "FAIL: origin inside box";
        if (intersect(origin, new Vec3(0, 0, 0), near, 0, 20) != null)
            return "FAIL: zero direction";
        return "PASS: nearest hit, parallel axes, behind-ray rejection, inside-box origin and zero-direction guard.";
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
