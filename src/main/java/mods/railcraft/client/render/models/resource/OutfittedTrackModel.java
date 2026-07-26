/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2020
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.client.render.models.resource;

import mods.railcraft.api.tracks.TrackKit;
import mods.railcraft.api.tracks.TrackRegistry;
import mods.railcraft.api.tracks.TrackType;
import mods.railcraft.client.render.world.TrackKitVisibilityManager;
import mods.railcraft.common.blocks.tracks.TrackShapeHelper;
import mods.railcraft.common.blocks.tracks.behaivor.TrackTypes;
import mods.railcraft.common.blocks.tracks.outfitted.BlockTrackOutfitted;
import mods.railcraft.common.blocks.tracks.outfitted.TrackKits;
import mods.railcraft.common.util.misc.Optionals;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Created by CovertJaguar on 8/18/2016 for Railcraft.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
@SideOnly(Side.CLIENT)
public class OutfittedTrackModel implements IModel {
    public static final OutfittedTrackModel INSTANCE = new OutfittedTrackModel();
    private static final String TRACK_TYPE_MODEL_FOLDER = "tracks/outfitted/type/";
    private static final String TRACK_KIT_MODEL_FOLDER = "tracks/outfitted/kit/";
    private static final String UNIFIED_MODEL_FOLDER = "tracks/outfitted/unified/";
    private static final Set<ResourceLocation> models = new HashSet<>();
    private static final Set<ModelResourceLocation> trackTypeModelsLocations = new HashSet<>();
    private static final Set<ModelResourceLocation> trackKitModelsLocations = new HashSet<>();
    private static final Set<ModelResourceLocation> unifiedModelsLocations = new HashSet<>();

    private ResourceLocation getModelLocation(String modelPrefix, ResourceLocation registryName) {
        return new ResourceLocation(
                registryName.getNamespace(),
                modelPrefix + registryName.getPath());
    }

    private ModelResourceLocation getTrackTypeModelLocation(TrackType trackType, BlockRailBase.EnumRailDirection shape) {
        return new ModelResourceLocation(getModelLocation(TRACK_TYPE_MODEL_FOLDER, trackType.getRegistryName()), "shape=" + shape.getName());
    }

    private ModelResourceLocation getTrackKitModelLocation(TrackKit trackKit, BlockRailBase.EnumRailDirection shape, int state) {
        return new ModelResourceLocation(getModelLocation(TRACK_KIT_MODEL_FOLDER, trackKit.getRegistryName()), "shape=" + shape.getName() + ",state=" + state);
    }

    private ModelResourceLocation getUnifiedModelLocation(TrackType trackType, TrackKit trackKit, BlockRailBase.EnumRailDirection shape, int state) {
        ResourceLocation trackTypeName = trackType.getRegistryName();
        ResourceLocation trackKitName = trackKit.getRegistryName();
        ResourceLocation modelLocation = new ResourceLocation(
                trackTypeName.getNamespace(),
                UNIFIED_MODEL_FOLDER + trackTypeName.getPath() + "/" + trackKitName.getPath()
        );
        return new ModelResourceLocation(modelLocation, "shape=" + shape.getName() + ",state=" + state);
    }

    @Override
    public Collection<ResourceLocation> getDependencies() {
        if (trackTypeModelsLocations.isEmpty()) {
            for (TrackType trackType : TrackRegistry.TRACK_TYPE) {
                for (BlockRailBase.EnumRailDirection shape : BlockTrackOutfitted.SHAPE.getAllowedValues()) {
                    trackTypeModelsLocations.add(getTrackTypeModelLocation(trackType, shape));
                }
            }
        }
        if (trackKitModelsLocations.isEmpty()) {
            TrackRegistry.TRACK_KIT.stream()
                    .filter(t -> t.getRenderer() == TrackKit.Renderer.COMPOSITE)
                    .forEach(t -> {
                                EnumSet<BlockRailBase.EnumRailDirection> shapes = EnumSet.copyOf(BlockTrackOutfitted.SHAPE.getAllowedValues());
                                if (!t.isAllowedOnSlopes()) {
                                    shapes.removeIf(s -> !TrackShapeHelper.isLevelStraight(s));
                                }
                                for (BlockRailBase.EnumRailDirection shape : shapes) {
                                    for (int state = 0; state < t.getRenderStates(); state++)
                                        trackKitModelsLocations.add(getTrackKitModelLocation(t, shape, state));
                                }
                            }
                    );
        }
        if (unifiedModelsLocations.isEmpty()) {
            TrackRegistry.TRACK_KIT.stream()
                    .filter(t -> t.getRenderer() == TrackKit.Renderer.UNIFIED)
                    .forEach(trackKit -> {
                                EnumSet<BlockRailBase.EnumRailDirection> shapes = EnumSet.copyOf(BlockTrackOutfitted.SHAPE.getAllowedValues());
                                if (!trackKit.isAllowedOnSlopes()) {
                                    shapes.removeIf(s -> !TrackShapeHelper.isLevelStraight(s));
                                }
                                for (TrackType trackType : TrackRegistry.TRACK_TYPE)
                                    for (BlockRailBase.EnumRailDirection shape : shapes) {
                                        for (int state = 0; state < trackKit.getRenderStates(); state++)
                                            unifiedModelsLocations.add(getUnifiedModelLocation(trackType, trackKit, shape, state));
                                    }
                            }
                    );
        }
        if (models.isEmpty()) {
            models.addAll(trackTypeModelsLocations);
            models.addAll(trackKitModelsLocations);
            models.addAll(unifiedModelsLocations);
        }
        return models;
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return Collections.emptyList();
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        getDependencies();
        return new CompositeModel(
                bakeModels(format, bakedTextureGetter, trackTypeModelsLocations),
                bakeModels(format, bakedTextureGetter, trackKitModelsLocations),
                bakeModels(format, bakedTextureGetter, unifiedModelsLocations));
    }

    private Map<ModelResourceLocation, IBakedModel> bakeModels(
            VertexFormat format,
            Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter,
            Set<ModelResourceLocation> modelLocations) {
        Map<ModelResourceLocation, IBakedModel> models = new HashMap<>();
        for (ModelResourceLocation modelLocation : modelLocations) {
            IModel model = ModelManager.getModel(modelLocation);
            models.put(modelLocation, model.bake(model.getDefaultState(), format, bakedTextureGetter));
        }
        return models;
    }

    public enum Loader implements ICustomModelLoader {
        INSTANCE;

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {
        }

        @Override
        public boolean accepts(ResourceLocation modelLocation) {
            return Objects.equals(modelLocation.getNamespace(), "railcraft")
                    && modelLocation.getPath().contains("outfitted_rail");
        }

        @Override
        public IModel loadModel(ResourceLocation modelLocation) {
            return OutfittedTrackModel.INSTANCE;
        }
    }

    public class CompositeModel implements IBakedModel {
        private final Map<ModelResourceLocation, IBakedModel> trackTypeModels;
        private final Map<ModelResourceLocation, IBakedModel> mergedSleeperTrackTypeModels;
        private final Map<ModelResourceLocation, IBakedModel> trackKitModels;
        private final Map<ModelResourceLocation, IBakedModel> elevatedTrackKitModels;
        private final Map<ModelResourceLocation, IBakedModel> unifiedModels;
        private final IBakedModel baseModel;

        public CompositeModel(
                Map<ModelResourceLocation, IBakedModel> trackTypeModels,
                Map<ModelResourceLocation, IBakedModel> trackKitModels,
                Map<ModelResourceLocation, IBakedModel> unifiedModels) {
            this.trackTypeModels = trackTypeModels;
            mergedSleeperTrackTypeModels = new HashMap<>();
            trackTypeModels.forEach((location, model) -> mergedSleeperTrackTypeModels.put(location, new MergedSleeperModel(model)));
            this.trackKitModels = trackKitModels;
            elevatedTrackKitModels = new HashMap<>();
            trackKitModels.forEach((location, model) -> elevatedTrackKitModels.put(location, new ElevatedModel(model)));
            this.unifiedModels = unifiedModels;
            baseModel = trackTypeModels.get(getTrackTypeModelLocation(TrackTypes.IRON.getTrackType(), BlockRailBase.EnumRailDirection.NORTH_SOUTH));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            Optional<IExtendedBlockState> stateOptional = Optional.ofNullable(state)
                    .map(Optionals.toType(IExtendedBlockState.class));

            BlockRailBase.EnumRailDirection shape = stateOptional
                    .map(s -> s.getValue(BlockTrackOutfitted.SHAPE))
                    .orElse(BlockRailBase.EnumRailDirection.NORTH_SOUTH);

            int kitState = stateOptional
                    .map(s -> s.getValue(BlockTrackOutfitted.STATE))
                    .orElse(0);

            TrackType trackType = stateOptional
                    .map(s -> s.getValue(BlockTrackOutfitted.TRACK_TYPE))
                    .orElse(TrackTypes.IRON.getTrackType());

            TrackKit trackKit = stateOptional
                    .map(s -> s.getValue(BlockTrackOutfitted.TRACK_KIT))
                    .orElse(TrackRegistry.getMissingTrackKit());

            List<BakedQuad> quads = new ArrayList<>();
            switch (trackKit.getRenderer()) {
                case COMPOSITE:
                    boolean hiddenKit = isNormallyHidden(trackType, trackKit);
                    boolean showHiddenKit = hiddenKit && TrackKitVisibilityManager.isTrackAuraActive();
                    IBakedModel trackTypeModel = showHiddenKit
                            ? getMergedSleeperTrackTypeModel(trackType, shape)
                            : getTrackTypeModel(trackType, shape);
                    if (trackTypeModel != null) quads.addAll(trackTypeModel.getQuads(state, side, rand));
                    if (!hiddenKit || showHiddenKit) {
                        IBakedModel trackKitModel = showHiddenKit
                                ? getElevatedTrackKitModel(trackKit, shape, kitState)
                                : getTrackKitModel(trackKit, shape, kitState);
                        if (trackKitModel != null) quads.addAll(trackKitModel.getQuads(null, side, rand));
                    }
                    break;
                case UNIFIED:
                    IBakedModel unifiedModel = getUnifiedModel(trackType, trackKit, shape, kitState);
                    if (unifiedModel != null) quads.addAll(unifiedModel.getQuads(state, side, rand));
                    break;
            }
            return quads;
        }

        private @Nullable IBakedModel getTrackTypeModel(TrackType trackType, BlockRailBase.EnumRailDirection shape) {
            return trackTypeModels.get(getTrackTypeModelLocation(trackType, shape));
        }

        private @Nullable IBakedModel getMergedSleeperTrackTypeModel(TrackType trackType, BlockRailBase.EnumRailDirection shape) {
            return mergedSleeperTrackTypeModels.get(getTrackTypeModelLocation(trackType, shape));
        }

        private @Nullable IBakedModel getTrackKitModel(TrackKit trackKit, BlockRailBase.EnumRailDirection shape, int state) {
            return trackKitModels.get(getTrackKitModelLocation(trackKit, shape, state));
        }

        private @Nullable IBakedModel getElevatedTrackKitModel(TrackKit trackKit, BlockRailBase.EnumRailDirection shape, int state) {
            return elevatedTrackKitModels.get(getTrackKitModelLocation(trackKit, shape, state));
        }

        private @Nullable IBakedModel getUnifiedModel(TrackType trackType, TrackKit trackKit, BlockRailBase.EnumRailDirection shape, int state) {
            return unifiedModels.get(getUnifiedModelLocation(trackType, trackKit, shape, state));
        }

        @Override
        public boolean isAmbientOcclusion() {
            return baseModel.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return baseModel.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return baseModel.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return baseModel.getParticleTexture();
        }

        @Override
        @Deprecated
        public ItemCameraTransforms getItemCameraTransforms() {
            return baseModel.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return baseModel.getOverrides();
        }
    }

    private static class MergedSleeperModel implements IBakedModel {
        // Match the full-width sleeper envelope and texture used by the custom turnout models.
        private static final String SLEEPER_TEXTURE = "nebulaecraft:blocks/rail_bed";
        private static final float LEFT_INNER_EDGE = 4.5F / 16.0F;
        private static final float RIGHT_INNER_EDGE = 11.5F / 16.0F;
        private static final float CENTER = 8.0F / 16.0F;
        private static final float EPSILON = 0.0001F;
        private final IBakedModel delegate;
        private final List<BakedQuad> generalQuads;
        private final Map<EnumFacing, List<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);

        MergedSleeperModel(IBakedModel delegate) {
            this.delegate = delegate;
            generalQuads = mergeSleepers(delegate.getQuads(null, null, 0));
            for (EnumFacing face : EnumFacing.VALUES)
                faceQuads.put(face, mergeSleepers(delegate.getQuads(null, face, 0)));
        }

        private static List<BakedQuad> mergeSleepers(List<BakedQuad> quads) {
            List<BakedQuad> merged = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                if (!SLEEPER_TEXTURE.equals(quad.getSprite().getIconName())) {
                    merged.add(quad);
                    continue;
                }

                int[] vertexData = quad.getVertexData().clone();
                VertexFormat format = quad.getFormat();
                int vertexSize = format.getIntegerSize();
                int positionOffset = findPositionOffset(format);
                for (int vertex = 0; vertex < 4; vertex++) {
                    int xIndex = vertex * vertexSize + positionOffset;
                    int zIndex = xIndex + 2;
                    vertexData[xIndex] = mergeInnerEdge(vertexData[xIndex]);
                    vertexData[zIndex] = mergeInnerEdge(vertexData[zIndex]);
                }
                merged.add(new BakedQuad(vertexData, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
                        quad.shouldApplyDiffuseLighting(), format));
            }
            return Collections.unmodifiableList(merged);
        }

        private static int mergeInnerEdge(int coordinateBits) {
            float coordinate = Float.intBitsToFloat(coordinateBits);
            if (Math.abs(coordinate - LEFT_INNER_EDGE) < EPSILON
                    || Math.abs(coordinate - RIGHT_INNER_EDGE) < EPSILON)
                return Float.floatToRawIntBits(CENTER);
            return coordinateBits;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return side == null ? generalQuads : faceQuads.get(side);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return delegate.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return delegate.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return delegate.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return delegate.getParticleTexture();
        }

        @Override
        @Deprecated
        public ItemCameraTransforms getItemCameraTransforms() {
            return delegate.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return delegate.getOverrides();
        }
    }

    private static class ElevatedModel implements IBakedModel {
        // Rest the common kit bottom (Y=-0.05) on the full sleeper top (Y=0.3).
        private static final float ELEVATION = 0.35F / 16.0F;
        private final IBakedModel delegate;
        private final List<BakedQuad> generalQuads;
        private final Map<EnumFacing, List<BakedQuad>> faceQuads = new EnumMap<>(EnumFacing.class);

        ElevatedModel(IBakedModel delegate) {
            this.delegate = delegate;
            generalQuads = elevate(delegate.getQuads(null, null, 0));
            for (EnumFacing face : EnumFacing.VALUES)
                faceQuads.put(face, elevate(delegate.getQuads(null, face, 0)));
        }

        private static List<BakedQuad> elevate(List<BakedQuad> quads) {
            List<BakedQuad> elevated = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                int[] vertexData = quad.getVertexData().clone();
                VertexFormat format = quad.getFormat();
                int vertexSize = format.getIntegerSize();
                int positionOffset = findPositionOffset(format);
                for (int vertex = 0; vertex < 4; vertex++) {
                    int yIndex = vertex * vertexSize + positionOffset + 1;
                    float y = Float.intBitsToFloat(vertexData[yIndex]);
                    vertexData[yIndex] = Float.floatToRawIntBits(y + ELEVATION);
                }
                elevated.add(new BakedQuad(vertexData, quad.getTintIndex(), quad.getFace(), quad.getSprite(),
                        quad.shouldApplyDiffuseLighting(), format));
            }
            return Collections.unmodifiableList(elevated);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
            return side == null ? generalQuads : faceQuads.get(side);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return delegate.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return delegate.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return delegate.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return delegate.getParticleTexture();
        }

        @Override
        @Deprecated
        public ItemCameraTransforms getItemCameraTransforms() {
            return delegate.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {
            return delegate.getOverrides();
        }
    }

    private static int findPositionOffset(VertexFormat format) {
        for (int element = 0; element < format.getElementCount(); element++) {
            if (format.getElement(element).isPositionElement())
                return format.getOffset(element) / Integer.BYTES;
        }
        throw new IllegalArgumentException("Vertex format has no position element");
    }

    private static boolean isNormallyHidden(TrackType trackType, TrackKit trackKit) {
        return trackKit != TrackKits.BUFFER_STOP.getTrackKit()
                && (trackType == TrackTypes.ELECTRIC.getTrackType()
                || trackType == TrackTypes.HIGH_SPEED.getTrackType()
                || trackType == TrackTypes.HIGH_SPEED_ELECTRIC.getTrackType()
                || trackType == TrackTypes.REINFORCED.getTrackType());
    }

}
