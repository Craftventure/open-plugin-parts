package net.craftventure.core.utils

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import net.craftventure.annotationkit.GenerateService
import net.craftventure.audioserver.packet.PacketPolygonOverlayAdd
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.core.animation.curve.LineairPointCurve
import net.craftventure.core.database.metadata.itemuse.*
import net.craftventure.core.database.metadata.itemwear.ItemWearEffect
import net.craftventure.core.database.metadata.itemwear.PeriodicItemWearEffect
import net.craftventure.core.database.metadata.itemwear.SpawnItemItemWearEffect
import net.craftventure.core.database.metadata.itemwear.SpawnParticleItemWearEffect
import net.craftventure.core.feature.balloon.extensions.*
import net.craftventure.core.feature.balloon.physics.BabyYodaBalloonPhysics
import net.craftventure.core.feature.balloon.physics.BalloonPhysics
import net.craftventure.core.feature.balloon.physics.DefaultBalloonPhysics
import net.craftventure.core.feature.balloon.physics.StaticBalloonPhysics
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.feature.finalevent.FinaleCinematicConfig
import net.craftventure.core.feature.finalevent.PlayerRecordingActionDto
import net.craftventure.core.feature.minigame.BaseLobby
import net.craftventure.core.feature.minigame.Lobby
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.autopia.Autopia
import net.craftventure.core.feature.minigame.snowball.SnowballFight
import net.craftventure.core.json.DoubleStringAdapter
import net.craftventure.core.ktx.json.MoshiBase
import net.craftventure.core.ktx.service.LoadService
import net.craftventure.core.ride.queue.RideQueue
import net.craftventure.core.ride.shooter.hitaction.DeltaBankAccountBalanceAction
import net.craftventure.core.ride.shooter.hitaction.DeltaScoreHitAction
import net.craftventure.core.ride.shooter.hitaction.DespawnHitAction
import net.craftventure.core.ride.shooter.hitaction.EntityHitAction
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCarJson
import net.craftventure.core.ride.trackedride.car.SpinningCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.car.seat.MinecartSeat
import net.craftventure.core.ride.trackedride.car.seat.Seat
import net.craftventure.core.ride.trackedride.config.PreTrainUpdateListenerJson
import net.craftventure.core.ride.trackedride.config.SwitchAtDistance
import net.craftventure.core.ride.trackedride.config.TrackedRideAddOn
import net.craftventure.core.ride.trackedride.config.addon.DistanceTrackerAddOn
import net.craftventure.core.ride.trackedride.segment.*
import net.craftventure.core.ride.tracklessride.programpart.*
import net.craftventure.core.ride.tracklessride.programpart.action.*
import net.craftventure.core.ride.tracklessride.programpart.data.ProgramPartData
import net.craftventure.core.ride.tracklessride.scene.SceneData
import net.craftventure.core.ride.tracklessride.scene.TracklessDefaultScene
import net.craftventure.core.ride.tracklessride.scene.TracklessStationScene
import net.craftventure.core.ride.tracklessride.scene.trigger.*
import net.craftventure.core.ride.tracklessride.track.PathPart
import net.craftventure.core.ride.tracklessride.track.SplinedPathPart
import net.craftventure.core.ride.tracklessride.transport.car.CarConfig
import net.craftventure.core.ride.tracklessride.transport.car.RiggedTracklessCar
import net.craftventure.core.ride.tracklessride.transport.car.TestTracklessRideCar

@GenerateService
class MainPluginLoadService : LoadService {
    override fun init() {
        MoshiBase.withBuilder()
            .add(LineairPointCurve.JsonAdapter())
            .add(DoubleStringAdapter())
            .add(
                PolymorphicJsonAdapterFactory.of(ProgramPartData::class.java, "type")
                    .withSubtype(TagSemaphoreStartProgramPart.Data::class.java, TagSemaphoreStartProgramPart.type)
                    .withSubtype(TagSemaphoreStopProgramPart.Data::class.java, TagSemaphoreStopProgramPart.type)
                    .withSubtype(NavigateProgramPart.Data::class.java, NavigateProgramPart.type)
                    .withSubtype(AddTagProgramPart.Data::class.java, AddTagProgramPart.type)
                    .withSubtype(RemoveTagProgramPart.Data::class.java, RemoveTagProgramPart.type)
                    .withSubtype(WaitProgramPart.Data::class.java, WaitProgramPart.type)
                    .withSubtype(TagConditionProgramPart.Data::class.java, TagConditionProgramPart.type)
                    .withSubtype(
                        GroupCarsTagSemaphoreProgramPart.Data::class.java,
                        GroupCarsTagSemaphoreProgramPart.type
                    )
                    .withSubtype(SwitchSceneProgramPart.Data::class.java, SwitchSceneProgramPart.type)
                    .withSubtype(QueueSceneProgramPart.Data::class.java, QueueSceneProgramPart.type)
                    .withSubtype(StartShooterSceneProgramPart.Data::class.java, StartShooterSceneProgramPart.type)
                    .withSubtype(StopShooterSceneProgramPart.Data::class.java, StopShooterSceneProgramPart.type)
                    .withSubtype(StartShooterTeamProgramPart.Data::class.java, StartShooterTeamProgramPart.type)
                    .withSubtype(StopShooterTeamProgramPart.Data::class.java, StopShooterTeamProgramPart.type)
                    .withSubtype(StationHoldProgramPart.Data::class.java, StationHoldProgramPart.type)
                    .withSubtype(ExitsToOverrideProgramPart.Data::class.java, ExitsToOverrideProgramPart.type)
                    .withSubtype(AllowSceneSwitchProgramPart.Data::class.java, AllowSceneSwitchProgramPart.type)
                    .withSubtype(SetDoublePropertyProgramPart.Data::class.java, SetDoublePropertyProgramPart.type)
                    .withSubtype(ReorderCarsProgramPart.Data::class.java, ReorderCarsProgramPart.type)
                    .withSubtype(RunActionProgramPart.Data::class.java, RunActionProgramPart.type)
                    .withSubtype(
                        InvalidateImageHolderProgramPart.Data::class.java,
                        InvalidateImageHolderProgramPart.type
                    )
                    .withSubtype(SaveTeamScoreProgramPart.Data::class.java, SaveTeamScoreProgramPart.type)
                    .withSubtype(StartAudioProgramPart.Data::class.java, StartAudioProgramPart.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(SceneData::class.java, "type")
                    .withSubtype(TracklessStationScene.Data::class.java, TracklessStationScene.type)
                    .withSubtype(TracklessDefaultScene.Data::class.java, TracklessDefaultScene.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(TrackedRideAddOn::class.java, "type")
                    .withSubtype(DistanceTrackerAddOn::class.java, DistanceTrackerAddOn.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(PathPart.Data::class.java, "type")
                    .withSubtype(SplinedPathPart.Data::class.java, SplinedPathPart.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(EntityHitAction.Data::class.java, "type")
                    .withSubtype(DeltaScoreHitAction.Data::class.java, DeltaScoreHitAction.type)
                    .withSubtype(DespawnHitAction.Data::class.java, DespawnHitAction.type)
                    .withSubtype(DeltaBankAccountBalanceAction.Data::class.java, DeltaBankAccountBalanceAction.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(SceneTriggerData::class.java, "type")
                    .withSubtype(AtAbsoluteDistanceSceneTrigger.Data::class.java, AtAbsoluteDistanceSceneTrigger.type)
                    .withSubtype(AtTimeInProgramSceneTrigger.Data::class.java, AtTimeInProgramSceneTrigger.type)
                    .withSubtype(ProgressSceneTrigger.Data::class.java, ProgressSceneTrigger.type)
                    .withSubtype(InAreaProgramSceneTrigger.Data::class.java, InAreaProgramSceneTrigger.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(ActionData::class.java, "type")
                    .withSubtype(SetDoublePropertyAction.Data::class.java, SetDoublePropertyAction.type)
                    .withSubtype(SetBooleanPropertyAction.Data::class.java, SetBooleanPropertyAction.type)
                    .withSubtype(FollowTrackPropertyAction.Data::class.java, FollowTrackPropertyAction.type)
                    .withSubtype(ScriptsAction.Data::class.java, ScriptsAction.type)
                    .withSubtype(SpecialEffectAction.Data::class.java, SpecialEffectAction.type)
                    .withSubtype(DoorAction.Data::class.java, DoorAction.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(Area.Json::class.java, "type")
                    .withSubtype(SimpleArea.Json::class.java, "simple")
                    .withSubtype(SimpleArea.JsonLegacy::class.java, "legacy")
                    .withSubtype(CombinedArea.Json::class.java, "combined")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(TrackSegmentJson::class.java, "type")
                    .withSubtype(SplinedTrackSegmentJson::class.java, "splinedsegment")
                    .withSubtype(StationSegment.Json::class.java, "stationsegment")
                    .withSubtype(InvertedSegment.Json::class.java, "inverted")
                    .withSubtype(ForkRouterSegment.Json::class.java, "forkrouter")
                    .withSubtype(TransportSegment.Json::class.java, "transport")
                    .withSubtype(LaunchSegmentJson::class.java, "launch")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(RideCar.Json::class.java, "type")
                    .withSubtype(DynamicSeatedRideCarJson::class.java, "dynamicseated")
                    .withSubtype(SpinningCar.Json::class.java, "spinning")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(Seat.Json::class.java, "type")
                    .withSubtype(MinecartSeat.Json::class.java, "minecart")
                    .withSubtype(ArmorStandSeat.Json::class.java, "armorstand")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(Lobby.Json::class.java, "type")
                    .withSubtype(BaseLobby.Json::class.java, "default")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(Minigame.Json::class.java, "type")
                    .withSubtype(SnowballFight.Json::class.java, "snowfight")
                    .withSubtype(Autopia.Json::class.java, "autopia")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(Lobby.Json::class.java, "type")
                    .withSubtype(BaseLobby.Json::class.java, "default")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(PreTrainUpdateListenerJson::class.java, "type")
                    .withSubtype(SwitchAtDistance::class.java, "switch_at_distance")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(DistanceListenerJson::class.java, "type")
                    .withSubtype(EjectAtDistance::class.java, "eject_at_distance")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(RideQueue.TrackedRideBoardingDelegate.Json::class.java, "type")
                    .withSubtype(RideQueue.RideStationBoardingDelegate.Json::class.java, "station_delegate")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(RideQueue.TracklesssRideBoardingDelegate.Json::class.java, "type")
                    .withSubtype(RideQueue.StationSceneBoardingDelegate.Json::class.java, "station_delegate")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(PacketPolygonOverlayAdd.AreaOverlay::class.java, "type")
                    .withSubtype(PacketPolygonOverlayAdd.RectangleOverlay::class.java, "rectangle")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(CarConfig::class.java, "type")
                    .withSubtype(RiggedTracklessCar.Config::class.java, RiggedTracklessCar.Config.type)
                    .withSubtype(TestTracklessRideCar.Config::class.java, TestTracklessRideCar.Config.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(ExtensibleBalloon.Extension.Json::class.java, "type")
                    .withSubtype(LeashExtension.Json::class.java, LeashExtension.Json.type)
                    .withSubtype(DebugExtension.Json::class.java, DebugExtension.Json.type)
                    .withSubtype(PopExtension.Json::class.java, PopExtension.Json.type)
                    .withSubtype(EntityExtension.Json::class.java, EntityExtension.Json.type)
                    .withSubtype(SpawnParticleExtension.Json::class.java, SpawnParticleExtension.Json.type)
                    .withSubtype(ArmatureBalloonExtension.Json::class.java, ArmatureBalloonExtension.Json.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(BalloonPhysics.Json::class.java, "type")
                    .withSubtype(BabyYodaBalloonPhysics.Json::class.java, BabyYodaBalloonPhysics.Json.type)
                    .withSubtype(DefaultBalloonPhysics.Json::class.java, DefaultBalloonPhysics.Json.type)
                    .withSubtype(StaticBalloonPhysics.Json::class.java, StaticBalloonPhysics.Json.type)
            )
            .add(
                PolymorphicJsonAdapterFactory.of(ItemUseEffect::class.java, "type")
                    .withSubtype(SoundItemUseEffect::class.java, "sound")
                    .withSubtype(FireworkItemUseEffect::class.java, "firework")
                    .withSubtype(ProjectileItemUseEffect::class.java, "projectile")
                    .withSubtype(AlcoholicConsumptionEffect::class.java, "alcoholic")
                    .withSubtype(PotionConsumptionItemUseEffect::class.java, "potion")
                    .withSubtype(AtAtBlasterItemUseEffect::class.java, "atatblaster")
                    .withSubtype(AchievementIncrementItemUseEffect::class.java, "achievement_increment")
                    .withSubtype(AchievementRewardItemUseEffect::class.java, "achievement_reward")
                    .withSubtype(SpawnItemItemUseEffect::class.java, "spawn_item")
                    .withSubtype(SpawnParticleItemUseEffect::class.java, "spawn_particle")
                    .withSubtype(DelayedItemUseEffect::class.java, "delayed")
                    .withSubtype(ActivateCoinBoosterItemUseEffect::class.java, "activate_personal_coinbooster")
                    .withSubtype(ActivateServerCoinBoosterItemUseEffect::class.java, "activate_server_coinbooster")
                    .withSubtype(SendChatItemUseEffect::class.java, "send_chat")
                    .withSubtype(ChancedItemUseEffect::class.java, "chanced")
                    .withSubtype(PukeItemUseEffect::class.java, "puke")
                    .withSubtype(FartItemUseEffect::class.java, "fart")
                    .withSubtype(RewardItemItemUseEffect::class.java, "reward_item")
                    .withSubtype(VelocityItemUseEffect::class.java, "apply_velocity")
                    .withSubtype(AddWearOffItemUseEffect::class.java, "add_wear_off")
                    .withSubtype(WearOffExpressionItemUseEffect::class.java, "wear_off_expression")
                    .withSubtype(ClearPotionItemUseEffect::class.java, "clear_potion_effect")
                    .withSubtype(PlayerEffectsItemUseEffect::class.java, "player_effects")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(ItemWearEffect::class.java, "type")
                    .withSubtype(SpawnItemItemWearEffect::class.java, "spawn_item")
                    .withSubtype(SpawnParticleItemWearEffect::class.java, "spawn_particle")
                    .withSubtype(PeriodicItemWearEffect::class.java, "periodic")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(PlayerRecordingActionDto::class.java, "type")
                    .withSubtype(PlayerRecordingActionDto.Location::class.java, "location")
                    .withSubtype(PlayerRecordingActionDto.Pose::class.java, "pose")
                    .withSubtype(PlayerRecordingActionDto.Item::class.java, "item")
                    .withSubtype(PlayerRecordingActionDto.PlayerAnimation::class.java, "player_animation")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(FinaleCinematicConfig.SceneAction::class.java, "type")
                    .withSubtype(FinaleCinematicConfig.SceneAction.FadeEnd::class.java, "fade_end")
                    .withSubtype(FinaleCinematicConfig.SceneAction.FadeStart::class.java, "fade_start")
                    .withSubtype(FinaleCinematicConfig.SceneAction.SetTime::class.java, "set_time")
                    .withSubtype(FinaleCinematicConfig.SceneAction.AnimateTime::class.java, "animate_time")
                    .withSubtype(FinaleCinematicConfig.SceneAction.PlaySpecialEffect::class.java, "special_effect_play")
                    .withSubtype(FinaleCinematicConfig.SceneAction.ScriptStart::class.java, "scene_start")
                    .withSubtype(FinaleCinematicConfig.SceneAction.AssumeRunning::class.java, "scene_assumed")
                    .withSubtype(FinaleCinematicConfig.SceneAction.ScriptStop::class.java, "scene_stop")
                    .withSubtype(FinaleCinematicConfig.SceneAction.DispatchRide::class.java, "dispatch_ride")
                    .withSubtype(FinaleCinematicConfig.SceneAction.FadeInText::class.java, "fade_in_text")
                    .withSubtype(FinaleCinematicConfig.SceneAction.FadeOutText::class.java, "fade_out_text")
                    .withSubtype(FinaleCinematicConfig.SceneAction.HardcodedAction::class.java, "hardcoded_action")
                    .withSubtype(FinaleCinematicConfig.SceneAction.Mount::class.java, "mount")
                    .withSubtype(FinaleCinematicConfig.SceneAction.Eating::class.java, "eating")
                    .withSubtype(FinaleCinematicConfig.SceneAction.Sit::class.java, "sit")
            )
    }
}