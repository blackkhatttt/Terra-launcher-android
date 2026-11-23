package com.universe.solarcrystal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Minimal immutable voxel world model for legacy-client compatibility work.
 * It stores unit blocks and exposes them to the CPU picking scene.
 * It is not connected to RubyDung's renderer, terrain generator or game loop.
 */
public final class RubyDungBlockWorld {
    private final List<Block> blocks;

    public RubyDungBlockWorld() {
        this.blocks = Collections.emptyList();
    }

    private RubyDungBlockWorld(List<Block> blocks) {
        this.blocks = Collections.unmodifiableList(new ArrayList<Block>(blocks));
    }

    public static final class Block {
        public final int x;
        public final int y;
        public final int z;
        public final String material;

        public Block(int x, int y, int z, String material) {
            if (material == null || material.trim().length() == 0)
                throw new IllegalArgumentException("Block material must not be empty");
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
        }

        public String id() {
            return "block:" + x + ":" + y + ":" + z;
        }
    }

    public static final class Selection {
        public final Block block;
        public final double distance;
        public final RubyDungRayPicker.Vec3 point;

        private Selection(Block block, double distance, RubyDungRayPicker.Vec3 point) {
            this.block = block;
            this.distance = distance;
            this.point = point;
        }
    }

    public int size() {
        return blocks.size();
    }

    public List<Block> blocks() {
        return blocks;
    }

    public Block getBlock(int x, int y, int z) {
        for (Block block : blocks) {
            if (block.x == x && block.y == y && block.z == z) return block;
        }
        return null;
    }

    public RubyDungBlockWorld withBlock(Block block) {
        if (block == null) throw new IllegalArgumentException("Block must not be null");
        if (getBlock(block.x, block.y, block.z) != null)
            throw new IllegalArgumentException("Block position already occupied: " + block.id());
        List<Block> copy = new ArrayList<Block>(blocks);
        copy.add(block);
        return new RubyDungBlockWorld(copy);
    }

    public RubyDungBlockWorld withoutBlock(int x, int y, int z) {
        List<Block> copy = new ArrayList<Block>(blocks);
        for (int i = 0; i < copy.size(); i++) {
            Block block = copy.get(i);
            if (block.x == x && block.y == y && block.z == z) {
                copy.remove(i);
                return new RubyDungBlockWorld(copy);
            }
        }
        return this;
    }

    public RubyDungPickingScene toPickingScene() {
        RubyDungPickingScene scene = new RubyDungPickingScene();
        for (Block block : blocks) {
            scene = scene.withTarget(new RubyDungPickingScene.Target(block.id(),
                new RubyDungRayPicker.Aabb(
                    new RubyDungRayPicker.Vec3(block.x, block.y, block.z),
                    new RubyDungRayPicker.Vec3(block.x + 1.0, block.y + 1.0, block.z + 1.0))));
        }
        return scene;
    }

    public Selection pick(RubyDungRayPicker.Vec3 origin,
                          RubyDungRayPicker.Vec3 direction,
                          double maxDistance) {
        RubyDungPickingScene.Selection hit =
            toPickingScene().pick(origin, direction, maxDistance);
        if (hit == null) return null;
        for (Block block : blocks) {
            if (block.id().equals(hit.id))
                return new Selection(block, hit.distance, hit.point);
        }
        return null;
    }

    public static String selfTest() {
        RubyDungBlockWorld world = new RubyDungBlockWorld()
            .withBlock(new Block(5, 0, 0, "stone"))
            .withBlock(new Block(2, 0, 0, "grass"));
        Selection hit = world.pick(new RubyDungRayPicker.Vec3(0, 0.5, 0.5),
            new RubyDungRayPicker.Vec3(1, 0, 0), 20);
        if (hit == null || hit.block.x != 2 || !"grass".equals(hit.block.material))
            return "FAIL: nearest voxel selection";
        if (world.size() != 2) return "FAIL: immutable world state";
        RubyDungBlockWorld edited = world.withoutBlock(2, 0, 0);
        if (edited.size() != 1 || world.size() != 2)
            return "FAIL: immutable block removal";
        if (edited.pick(new RubyDungRayPicker.Vec3(0, 0.5, 0.5),
                new RubyDungRayPicker.Vec3(1, 0, 0), 20).block.x != 5)
            return "FAIL: selection after block removal";
        return "PASS: voxel storage, block bounds, nearest block picking and immutable removal.";
    }
}
