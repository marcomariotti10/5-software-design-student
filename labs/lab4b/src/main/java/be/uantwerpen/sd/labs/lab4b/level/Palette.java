package be.uantwerpen.sd.labs.lab4b.level;

import javafx.scene.paint.Color;

public class Palette {
    public final Color background, floor, wall, target, player, box;

    private Palette(Color background, Color floor, Color wall, Color target, Color player, Color box) {
        this.background = background;
        this.floor = floor;
        this.wall = wall;
        this.target = target;
        this.player = player;
        this.box = box;
    }

    public Color getBackground() {
        return background;
    }

    public Color getFloor() {
        return floor;
    }

    public Color getWall() {
        return wall;
    }

    public Color getTarget() {
        return target;
    }

    public Color getPlayer() {
        return player;
    }

    public Color getBox() {
        return box;
    }

    public static class Builder {
        private Color background = Color.BLACK, floor = Color.GRAY, wall = Color.DARKGRAY;
        private Color target = Color.GOLD, player = Color.WHITE, box = Color.SADDLEBROWN;

        public Builder background(Color c) {
            this.background = c;
            return this;
        }

        public Builder floor(Color c) {
            this.floor = c;
            return this;
        }

        public Builder wall(Color c) {
            this.wall = c;
            return this;
        }

        public Builder target(Color c) {
            this.target = c;
            return this;
        }

        public Builder player(Color c) {
            this.player = c;
            return this;
        }

        public Builder box(Color c) {
            this.box = c;
            return this;
        }

        public Palette build() {
            return new Palette(background, floor, wall, target, player, box);
        }
    }
}

