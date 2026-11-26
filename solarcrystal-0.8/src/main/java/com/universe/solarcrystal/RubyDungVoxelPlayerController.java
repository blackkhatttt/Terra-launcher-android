package com.universe.solarcrystal;

/**
 * Small first-person controller for the generated voxel renderer fixture.
 * Uses axis-separated AABB collision; it is not wired to RubyDung physics.
 */
public final class RubyDungVoxelPlayerController {
    private static final double RADIUS = 0.30;
    private static final double HEIGHT = 1.75;
    private static final double WALK_SPEED = 4.2;
    private static final double GRAVITY = 18.0;
    private static final double JUMP_SPEED = 7.0;

    private final RubyDungBlockWorld world;
    private double x, y, z;
    private double velocityY;
    private double yaw;
    private double pitch;
    private boolean grounded;

    public RubyDungVoxelPlayerController(RubyDungBlockWorld world,
                                         double spawnX, double spawnY, double spawnZ) {
        if (world == null) throw new IllegalArgumentException("World must not be null");
        this.world = world;
        this.x = spawnX;
        this.y = spawnY;
        this.z = spawnZ;
        if (collides(x, y, z)) throw new IllegalArgumentException("Spawn position intersects terrain");
    }

    public void rotate(double yawDelta, double pitchDelta) {
        yaw += yawDelta;
        pitch = clamp(pitch + pitchDelta, -85.0, 85.0);
    }

    public void tick(double forward, double strafe, boolean jump, double deltaSeconds) {
        double dt = clamp(deltaSeconds, 0.0, 0.05);
        forward = clamp(forward, -1.0, 1.0);
        strafe = clamp(strafe, -1.0, 1.0);
        double length = Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0) { forward /= length; strafe /= length; }

        double radians = Math.toRadians(yaw);
        double dx = (Math.sin(radians) * forward + Math.cos(radians) * strafe)
            * WALK_SPEED * dt;
        double dz = (-Math.cos(radians) * forward + Math.sin(radians) * strafe)
            * WALK_SPEED * dt;
        moveHorizontal(dx, 0.0, dz);

        if (jump && grounded) {
            velocityY = JUMP_SPEED;
            grounded = false;
        }
        velocityY -= GRAVITY * dt;
        double nextY = y + velocityY * dt;
        if (!collides(x, nextY, z)) {
            y = nextY;
            grounded = false;
        } else if (velocityY < 0.0) {
            grounded = true;
            velocityY = 0.0;
            while (!collides(x, y - 0.01, z)) y -= 0.01;
        } else {
            velocityY = 0.0;
        }
    }

    private void moveHorizontal(double dx, double dy, double dz) {
        if (!collides(x + dx, y, z)) x += dx;
        if (!collides(x, y, z + dz)) z += dz;
    }

    private boolean collides(double px, double py, double pz) {
        int minX = (int) Math.floor(px - RADIUS);
        int maxX = (int) Math.floor(px + RADIUS);
        int minY = (int) Math.floor(py);
        int maxY = (int) Math.floor(py + HEIGHT - 0.001);
        int minZ = (int) Math.floor(pz - RADIUS);
        int maxZ = (int) Math.floor(pz + RADIUS);
        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++)
                    if (world.getBlock(bx, by, bz) != null) return true;
        return false;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double yaw() { return yaw; }
    public double pitch() { return pitch; }
    public boolean grounded() { return grounded; }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String selfTest() {
        RubyDungBlockWorld floor = new RubyDungBlockWorld();
        for (int x = -2; x <= 2; x++)
            for (int z = -2; z <= 2; z++)
                floor = floor.withBlock(new RubyDungBlockWorld.Block(x, 0, z, "stone"));
        RubyDungVoxelPlayerController player =
            new RubyDungVoxelPlayerController(floor, 0.5, 1.0, 0.5);
        for (int i = 0; i < 10; i++) player.tick(1.0, 0.0, false, 0.05);
        if (player.z() > -0.1) return "FAIL: forward movement and yaw orientation";
        if (player.y() < 0.99 || player.y() > 1.02)
            return "FAIL: gravity should settle on the floor";
        double beforeJump = player.y();
        player.tick(0.0, 0.0, true, 0.05);
        if (player.y() <= beforeJump) return "FAIL: jump impulse";
        player.rotate(0.0, 500.0);
        if (player.pitch() > 85.0) return "FAIL: pitch clamp";
        return "PASS: first-person movement, floor collision, gravity, jump and camera pitch limits.";
    }
}
