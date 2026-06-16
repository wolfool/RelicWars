package com.wolfool.relicwars.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * BetterModel 연동 유틸리티 (리플렉션 기반).
 * BetterModel JAR이 Java 25로 컴파일되어 있어 직접 import 불가.
 * 런타임에서 리플렉션으로 API를 호출합니다.
 * 
 * 사용법: BetterModel의 models 폴더에 .bbmodel 파일을 넣으면
 * 이 클래스가 자동으로 해당 모델을 엔티티에 적용합니다.
 * 
 * 예: plugins/BetterModel/models/greed_assassin.bbmodel
 */
public class BetterModelHook {

    private static Boolean available = null;

    /**
     * BetterModel API가 사용 가능한지 확인합니다.
     */
    private static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().isPluginEnabled("BetterModel");
        }
        return available;
    }

    /**
     * 엔티티에 BetterModel 커스텀 모델을 적용합니다.
     * BetterModel이 없거나 모델이 없으면 아무것도 하지 않습니다.
     * 
     * @param entity 모델을 적용할 엔티티
     * @param modelName 모델 이름 (.bbmodel 파일명, 확장자 제외)
     * @return 모델 적용 성공 여부
     */
    @SuppressWarnings("unchecked")
    public static boolean applyModel(Entity entity, String modelName) {
        if (!isAvailable()) return false;

        try {
            // BetterModelBukkit.platform()
            Class<?> betterModelBukkitClass = Class.forName("kr.toxicity.model.api.bukkit.BetterModelBukkit");
            Method platformMethod = betterModelBukkitClass.getMethod("platform");
            Object platform = platformMethod.invoke(null);

            // platform.model(modelName) -> Optional<ModelRenderer>
            Method modelMethod = platform.getClass().getMethod("model", String.class);
            Optional<?> optionalModel = (Optional<?>) modelMethod.invoke(platform, modelName);

            if (optionalModel.isEmpty()) {
                Bukkit.getLogger().info("[RelicWars] BetterModel 모델 '" + modelName + "'을 찾을 수 없습니다. (정상 — 모델 파일이 없으면 바닐라 외형)");
                return false;
            }

            Object model = optionalModel.get();

            // BukkitAdapter.adapt(entity)
            Class<?> adapterClass = Class.forName("kr.toxicity.model.api.bukkit.platform.BukkitAdapter");
            Method adaptMethod = adapterClass.getMethod("adapt", Entity.class);
            Object adaptedEntity = adaptMethod.invoke(null, entity);

            // TrackerModifier.DEFAULT
            Class<?> trackerModifierClass = Class.forName("kr.toxicity.model.api.tracker.TrackerModifier");
            Object defaultModifier = trackerModifierClass.getField("DEFAULT").get(null);

            // model.create(adaptedEntity, defaultModifier, tracker -> {})
            // 콜백 타입을 리플렉션으로 처리
            for (Method m : model.getClass().getMethods()) {
                if (m.getName().equals("create") && m.getParameterCount() == 3) {
                    // java.util.function.Consumer 람다를 만들어야 함
                    java.util.function.Consumer<Object> callback = tracker -> {};
                    m.invoke(model, adaptedEntity, defaultModifier, callback);
                    Bukkit.getLogger().info("[RelicWars] BetterModel 모델 '" + modelName + "' 적용 완료!");
                    return true;
                }
            }

            return false;
        } catch (ClassNotFoundException e) {
            // BetterModel API 클래스를 찾을 수 없음 — 정상 (미설치)
            available = false;
            return false;
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RelicWars] BetterModel 모델 적용 실패 (" + modelName + "): " + e.getMessage());
            return false;
        }
    }

    /**
     * 플러그인 리로드 시 캐시 초기화.
     */
    public static void resetCache() {
        available = null;
    }
}
