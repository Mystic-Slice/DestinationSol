/*
 * Copyright 2021 The Terasology Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.world.generators;

import com.badlogic.gdx.math.Vector2;
import org.destinationsol.world.Orbital;

/**
 * This class represents the abstract version of any feature which will populate the game's SolarSystems, such as Planets,
 * Mazes, or anything else which a user may wish to implement. Extend this class with a concrete class to create a
 * general Feature, or extend the child {@link PlanetGenerator} or {@link MazeGenerator} classes to create custom
 * versions of those elements.
*/
public abstract class FeatureGenerator {
    public static final float ORBITAL_FEATURE_BUFFER = 8f;
    private Vector2 position = new Vector2();
    private float radius;
    private boolean positioned;
    private Orbital orbital;

    public abstract void build();

    public void setPosition(Vector2 position) {
        this.position.set(position);
        setPositioned(true);
    }

    public Vector2 getPosition() {
        return position;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return radius;
    }

    public void setPositioned(boolean positioned) {
        this.positioned = positioned;
    }

    public boolean getPositioned() {
        return positioned;
    }

    public void setOrbital(Orbital orbital) {
        this.orbital = orbital;
    }

    public Orbital getOrbital() {
        return orbital;
    }

    public float getDiameter() {
        return 2 * radius;
    }
}
