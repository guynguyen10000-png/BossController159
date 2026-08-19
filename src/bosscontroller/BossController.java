package bosscontroller;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Timer;

import mindustry.Vars;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.mod.Mod;
import mindustry.type.UnitType;

public class BossController extends Mod {

    private static UnitType selectedBoss = UnitTypes.reign;

    /*
     * Các boss có sẵn trong GUI.
     *
     * Bạn có thể thêm UnitTypes khác vào đây.
     */
    private static final UnitType[] BOSS_LIST = {
        UnitTypes.scepter,
        UnitTypes.reign,
        UnitTypes.eclipse,
        UnitTypes.oct,
        UnitTypes.corvus,
        UnitTypes.toxopid
    };

    /*
     * Lưu buildSpeed/buildRange gốc để không thay đổi
     * vĩnh viễn UnitType nếu không cần.
     */
    private static final arc.struct.ObjectMap<UnitType, Float> oldBuildSpeed =
        new arc.struct.ObjectMap<>();

    private static final arc.struct.ObjectMap<UnitType, Float> oldBuildRange =
        new arc.struct.ObjectMap<>();

    public BossController() {

        /*
         * Chạy khi client đã load xong.
         */
        Events.on(ClientLoadEvent.class, event -> {

            /*
             * Nút xuất hiện ngay ở màn hình chính.
             */
            Vars.ui.menufrag.addButton(
                "Boss Controller",
                Icon.units,
                BossController::showBossMenu
            );
        });

        /*
         * Khi vào map.
         */
        Events.on(WorldLoadEvent.class, event -> {

            /*
             * Chờ một chút để player/unit được tạo hoàn chỉnh.
             */
            Timer.schedule(() -> {

                if (Vars.player == null) return;
                if (Vars.player.dead()) return;

                becomeBoss();

            }, 0.5f);
        });
    }

    /*
     * =========================
     * GUI CHỌN BOSS
     * =========================
     */

    private static void showBossMenu() {

        Dialog dialog = new Dialog("Boss Controller");

        dialog.cont.pane(table -> {

            table.defaults()
                .growX()
                .height(55f)
                .pad(4f);

            table.add(
                "[accent]BOSS CONTROLLER[/accent]"
            ).row();

            table.add(
                "Boss hiện tại: [accent]"
                    + selectedBoss.localizedName
                    + "[/accent]"
            ).row();

            table.add(
                "Chọn boss trước khi vào map."
            ).row();

            table.add().height(10f).row();

            for (UnitType type : BOSS_LIST) {

                if (type == null) continue;

                TextButton button = table.button(
                    type.localizedName,
                    () -> {

                        selectedBoss = type;

                        Core.app.post(() -> {
                            dialog.hide();
                        });

                        Vars.ui.showInfoToast(
                            "Đã chọn: "
                                + type.localizedName,
                            2f
                        );
                    }
                ).get();

                button.getLabel().setFontScale(1f);
            }

        }).grow();

        dialog.buttons.button(
            "Đóng",
            dialog::hide
        );

        dialog.show();
    }

    /*
     * =========================
     * BIẾN PLAYER THÀNH BOSS
     * =========================
     */

    private static void becomeBoss() {

        if (selectedBoss == null) {
            selectedBoss = UnitTypes.reign;
        }

        if (Vars.player == null) return;

        Unit oldUnit = Vars.player.unit();

        if (oldUnit == null) {
            /*
             * Player chưa có unit.
             * Thử lại sau.
             */
            Timer.schedule(
                BossController::becomeBoss,
                0.5f
            );

            return;
        }

        /*
         * Không tạo lại nếu player đã là boss.
         */
        if (oldUnit.type == selectedBoss) {
            enableBuilding(selectedBoss);
            return;
        }

        Team team = Vars.player.team();

        /*
         * Tạo boss cùng team với player.
         */
        Unit boss = selectedBoss.create(team);

        if (boss == null) return;

        /*
         * Cho boss khả năng xây.
         */
        enableBuilding(selectedBoss);

        /*
         * Đặt boss đúng vị trí player cũ.
         */
        boss.set(
            oldUnit.x,
            oldUnit.y
        );

        boss.rotation = oldUnit.rotation;

        /*
         * Thêm vào world.
         */
        boss.add();

        /*
         * Gán player vào unit mới.
         *
         * UnitType.create(Team) sẽ tạo controller
         * theo UnitType. Vì đây là unit của player,
         * player controller sẽ được dùng khi player
         * nhận unit này.
         */
        Vars.player.unit(boss);

        /*
         * Xóa unit cũ.
         */
        if (oldUnit.isAdded()) {
            oldUnit.remove();
        }

        Vars.ui.showInfoToast(
            "Bạn đang điều khiển: "
                + selectedBoss.localizedName,
            3f
        );
    }

    /*
     * =========================
     * CHO BOSS XÂY
     * =========================
     */

    private static void enableBuilding(UnitType type) {

        if (type == null) return;

        /*
         * Lưu giá trị gốc lần đầu tiên.
         */
        if (!oldBuildSpeed.containsKey(type)) {
            oldBuildSpeed.put(
                type,
                type.buildSpeed
            );
        }

        if (!oldBuildRange.containsKey(type)) {
            oldBuildRange.put(
                type,
                type.buildRange
            );
        }

        /*
         * Trong Mindustry 159.7:
         *
         * buildSpeed < 0 = không thể xây.
         *
         * buildSpeed >= 0 = có thể xây.
         */
        type.buildSpeed = 1f;

        /*
         * Tăng khoảng cách xây.
         */
        type.buildRange = 80f;
    }

    /*
     * Cho phép lấy boss hiện tại từ code khác
     * nếu sau này muốn thêm tính năng.
     */
    public static UnitType selectedBoss() {
        return selectedBoss;
    }
}
