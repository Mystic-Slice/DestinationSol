/*
 * Copyright 2018 MovingBlocks
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
package org.destinationsol.assets.textures;

import com.badlogic.gdx.graphics.Texture;
import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.DisposableResource;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.module.annotations.RegisterAssetType;
import org.terasology.nui.UITextureRegion;

@RegisterAssetType(folderName = {"textures", "ships", "items", "grounds", "mazes", "asteroids", "fonts"}, factoryClass = DSTextureFactory.class)
public class DSTexture extends Asset<DSTextureData> {
    private final TextureResources resources;
    private DSTextureData dsTextureData;

    public DSTexture(ResourceUrn urn, AssetType<?, DSTextureData> assetType, DSTextureData data, TextureResources resources) {
        super(urn, assetType, resources);
        this.resources = resources;
        reload(data);
    }

    public static DSTexture create(ResourceUrn urn, AssetType<?, DSTextureData> assetType, DSTextureData data) {
        return new DSTexture(urn, assetType, data, new TextureResources());
    }

    @Override
    protected void doReload(DSTextureData data) {
        this.dsTextureData = data;
        resources.texture = data.getTexture();
    }

    public Texture getTexture() {
        return dsTextureData.getTexture();
    }

    /**
     * Obtains the NUI {@link UITextureRegion} associated with this texture.
     * @return the associated UI texture
     */
    public UITextureRegion getUiTexture() {
        return dsTextureData;
    }

    private static class TextureResources implements DisposableResource {
        private Texture texture;

        /**
         * Closes the asset. It is expected this should only happen once.
         */
        @Override
        public void close() {
            texture.dispose();
        }
    }
}
