package io.github.magicrpg.client;

import io.github.magicrpg.MagicRpg;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo.Layer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

final class LilacArmorModel<S extends HumanoidRenderState> extends Model<S> {
    private final HumanoidModel<S> original;
    private final List<Attachment> attachments = new ArrayList<>();

    private LilacArmorModel(HumanoidModel<S> original, ModelPart relief, List<String> bones) {
        super(new ModelPart(List.of(), Map.of("armor", original.root(), "plates", relief)), original.renderType());
        this.original = original;
        attachments.add(new Attachment(original.root(), relief));
        for (String name : bones) {
            attachments.add(new Attachment(original.root().getChild(name), relief.getChild(name)));
        }
    }

    @Override
    public void setupAnim(S state) {
        original.setupAnim(state);
        for (Attachment attachment : attachments) {
            attachment.plate().loadPose(attachment.bone().storePose());
            attachment.plate().visible = attachment.bone().visible;
        }
    }

    private record Attachment(ModelPart bone, ModelPart plate) {}

    private static PartDefinition createRelief(EquipmentSlot slot) {
        var root = new MeshDefinition().getRoot();
        switch (slot) {
            case HEAD -> root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(17, 20).addBox(-3, -5.8F, -5.95F, 6, 2, 2), PartPose.ZERO);
            case CHEST -> {
                root.addOrReplaceChild("body", CubeListBuilder.create()
                    .texOffs(32, 0).addBox(-3, 2.25F, -3.9F, 6, 5, 2)
                    .texOffs(32, 7).addBox(-1, 3.2F, -4.25F, 2, 2, 1), PartPose.ZERO);
                root.addOrReplaceChild("right_arm", shoulder(-4.3F), PartPose.ZERO);
                root.addOrReplaceChild("left_arm", shoulder(-0.7F), PartPose.ZERO);
            }
            case LEGS -> {
                root.addOrReplaceChild("right_leg", knee(), PartPose.ZERO);
                root.addOrReplaceChild("left_leg", knee(), PartPose.ZERO);
            }
            case FEET -> {
                root.addOrReplaceChild("right_leg", toe(), PartPose.ZERO);
                root.addOrReplaceChild("left_leg", toe(), PartPose.ZERO);
            }
            default -> throw new IllegalArgumentException("Unsupported lilac armor slot " + slot);
        }
        return root;
    }

    private static CubeListBuilder shoulder(float x) {
        return CubeListBuilder.create().texOffs(49, 0).addBox(x, -3.45F, -3.7F, 5, 3, 2);
    }

    private static CubeListBuilder knee() {
        return CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, 4.3F, -3.25F, 3, 3, 2);
    }

    private static CubeListBuilder toe() {
        return CubeListBuilder.create().texOffs(48, 7).addBox(-1.5F, 9.6F, -3.8F, 3, 2, 2);
    }

    static final class Extension implements IClientItemExtensions {
        private static final Identifier HELMET_TEXTURE =
            MagicRpg.id("textures/entity/equipment/humanoid/lilac_helmet.png");

        private final EquipmentSlot slot;
        private final PartDefinition relief;
        private final List<String> bones;
        private final Map<HumanoidModel<?>, LilacArmorModel<?>> models = new IdentityHashMap<>();
        private EntityModelSet entityModels;

        Extension(EquipmentSlot slot) {
            this.slot = slot;
            relief = createRelief(slot);
            bones = relief.getChildren().stream().map(Map.Entry::getKey).toList();
        }

        @Override
        public Model<?> getHumanoidArmorModel(ItemStack stack, LayerType layer, Model original) {
            if ((layer != LayerType.HUMANOID && layer != LayerType.HUMANOID_LEGGINGS)
                || !(original instanceof HumanoidModel<?> humanoid)) {
                return original;
            }
            var currentModels = Minecraft.getInstance().getEntityModels();
            if (entityModels != currentModels) {
                models.clear();
                entityModels = currentModels;
            }
            // Each wrapper animates the original model when Minecraft draws the submitted armor.
            return models.computeIfAbsent(humanoid, source -> new LilacArmorModel<>(source, relief.bake(64, 32), bones));
        }

        @Override
        public Identifier getArmorTexture(ItemStack stack, LayerType type, Layer layer, Identifier fallback) {
            return slot == EquipmentSlot.HEAD && type == LayerType.HUMANOID ? HELMET_TEXTURE : fallback;
        }
    }
}
