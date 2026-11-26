package io.github.magicrpg.boss;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public sealed interface BossZone permits BossZone.Circle, BossZone.Cone, BossZone.Line, BossZone.Ring {
    double MAX_REACH = 48;
    double reach();

    boolean contains(double side, double forward);

    void outline(Placed zone, int points, Consumer<Vec3> visitor);

    default Placed at(Vec3 origin, float yaw, double halfHeight) {
        return new Placed(this, origin, yaw, halfHeight);
    }

    record Circle(double radius) implements BossZone {
        public Circle { requireSize(radius); }
        @Override public double reach() { return radius; }
        @Override public boolean contains(double side, double forward) {
            return side * side + forward * forward <= radius * radius;
        }
        @Override public void outline(Placed zone, int points, Consumer<Vec3> visitor) {
            arc(zone, radius, -Math.PI, Math.PI, points, false, visitor);
        }
    }

    record Cone(double radius, double halfAngleDegrees) implements BossZone {
        public Cone {
            requireSize(radius);
            if (!(halfAngleDegrees > 0 && halfAngleDegrees < 180)) {
                throw new IllegalArgumentException("Cone half-angle must be between 0 and 180 degrees");
            }
        }
        @Override public double reach() { return radius; }
        @Override public boolean contains(double side, double forward) {
            double length = Math.hypot(side, forward);
            return length <= radius && forward >= length * Math.cos(Math.toRadians(halfAngleDegrees));
        }
        @Override public void outline(Placed zone, int points, Consumer<Vec3> visitor) {
            double angle = Math.toRadians(halfAngleDegrees);
            int arcPoints = points / 2;
            arc(zone, radius, -angle, angle, arcPoints, true, visitor);
            int edgePoints = points - arcPoints;
            for (int i = 0; i < edgePoints; i++) {
                double distance = radius * (i / 2) / ((edgePoints + 1) / 2);
                visitor.accept(zone.point(Math.copySign(distance * Math.sin(angle), i % 2 == 0 ? 1 : -1),
                    distance * Math.cos(angle)));
            }
        }
    }

    record Line(double length, double halfWidth) implements BossZone {
        public Line {
            requireSize(length);
            requireSize(halfWidth);
            requireSize(Math.hypot(length, halfWidth));
        }
        @Override public double reach() { return Math.hypot(length, halfWidth); }
        @Override public boolean contains(double side, double forward) {
            return forward >= 0 && forward <= length && Math.abs(side) <= halfWidth;
        }
        @Override public void outline(Placed zone, int points, Consumer<Vec3> visitor) {
            double perimeter = 2 * length + 4 * halfWidth;
            for (int i = 0; i < points; i++) {
                double distance = perimeter * i / points;
                if (distance < length) {
                    visitor.accept(zone.point(-halfWidth, distance));
                } else if (distance < length + 2 * halfWidth) {
                    visitor.accept(zone.point(distance - length - halfWidth, length));
                } else if (distance < 2 * length + 2 * halfWidth) {
                    visitor.accept(zone.point(halfWidth, 2 * length + 2 * halfWidth - distance));
                } else {
                    visitor.accept(zone.point(2 * length + 3 * halfWidth - distance, 0));
                }
            }
        }
    }

    record Ring(double innerRadius, double outerRadius) implements BossZone {
        public Ring {
            requireSize(outerRadius);
            if (!(innerRadius >= 0 && innerRadius < outerRadius)) {
                throw new IllegalArgumentException("Invalid inner radius");
            }
        }
        @Override public double reach() { return outerRadius; }
        @Override public boolean contains(double side, double forward) {
            double squared = side * side + forward * forward;
            return squared >= innerRadius * innerRadius && squared <= outerRadius * outerRadius;
        }
        @Override public void outline(Placed zone, int points, Consumer<Vec3> visitor) {
            int innerPoints = innerRadius == 0 ? 0 : points / 2;
            arc(zone, outerRadius, -Math.PI, Math.PI, points - innerPoints, false, visitor);
            if (innerPoints > 0) {
                arc(zone, innerRadius, -Math.PI, Math.PI, innerPoints, false, visitor);
            }
        }
    }

    record Placed(BossZone shape, Vec3 origin, float yaw, double halfHeight) {
        public Placed {
            Objects.requireNonNull(shape);
            if (!(Double.isFinite(origin.x) && Double.isFinite(origin.y) && Double.isFinite(origin.z))
                || !Float.isFinite(yaw)) {
                throw new IllegalArgumentException("Zone position and yaw must be finite");
            }
            requireSize(halfHeight);
        }

        public AABB bounds() {
            double reach = shape.reach();
            return new AABB(origin.x - reach, origin.y - halfHeight, origin.z - reach,
                origin.x + reach, origin.y + halfHeight, origin.z + reach);
        }

        public boolean contains(Vec3 feet) {
            var forward = Vec3.directionFromRotation(0, yaw);
            double x = feet.x - origin.x;
            double z = feet.z - origin.z;
            return Math.abs(feet.y - origin.y) <= halfHeight
                && shape.contains(x * forward.z - z * forward.x, x * forward.x + z * forward.z);
        }

        public void outline(int points, Consumer<Vec3> visitor) {
            if (points < 8 || points > 96) {
                throw new IllegalArgumentException("Outline requires 8–96 points");
            }
            shape.outline(this, points, visitor);
        }

        private Vec3 point(double side, double distance) {
            var forward = Vec3.directionFromRotation(0, yaw);
            return origin.add(side * forward.z + distance * forward.x, 0, -side * forward.x + distance * forward.z);
        }
    }

    private static void requireSize(double value) {
        if (!(value > 0 && value <= MAX_REACH)) {
            throw new IllegalArgumentException("Zone size must be in (0, 48]");
        }
    }

    private static void arc(Placed zone, double radius, double from, double to, int points,
                            boolean endpoints, Consumer<Vec3> visitor) {
        for (int i = 0; i < points; i++) {
            double angle = from + (to - from) * i / (endpoints ? points - 1 : points);
            visitor.accept(zone.point(radius * Math.sin(angle), radius * Math.cos(angle)));
        }
    }
}
