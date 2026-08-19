package bosscontroller;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;

import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;

public class BossController extends Mod {

    // Boss được chọn.
    private static UnitType selectedBoss = null;

    // Tên boss mặc định.
    private static final UnitType DEFAULT_BOSS = UnitTypes.reign;

    // Những boss muốn cho xuất hiện trong GUI.
    private static final UnitType[] BOSSES = {
        UnitTypes.reign,
        UnitTypes.scepter,
        UnitTypes.eclipse,
        UnitTypes.oct,
        UnitTypes.corvus,
        UnitTypes.toxopid
    };

    // Lưu buildSpeed cũ để có thể khôi phục.
    private static final ObjectMap<UnitType, Float> oldBuildSpeed =
        new ObjectMap<>();

    public BossController() {

        // Boss mặc định.
        selectedBoss = DEFAULT_BOSS;

        // GUI menu chính.
        Events.on(ClientLoadEvent.class, event -> {
            createMenuButton();
        });

        // Khi vào map.
        Events.on(WorldLoadEvent.class, event -> {
            Timer.schedule(() -> {
                if (Vars.player == null) return;

                applySelectedBoss();
            }, 0.5f);
        });

        // Khi thoát map / reset.
        Events.on(WorldLoadEvent.class, event -> {
            Timer.schedule(() -> {
                if (Vars.player == null) return;
            }, 1f);
        });
    }

    /**
     * Tạo nút Boss Controller ở menu chính.
     */
    private void createMenuButton() {

        // Thêm nút vào fragment menu.
        Vars.ui.menufrag.addButton(
            "Boss Controller",
            Icon.units,
            BossController::showBossSelector
        );
    }

    /**
     * GUI chọn boss.
     */
    private static void showBossSelector() {

        BaseDialog dialog = new BaseDialog("Boss Controller");

        dialog.cont.pane(table -> {

            table.defaults()
                .growX()
                .height(55f)
                .pad(4f);

            table.add(
                "[accent]Chọn Boss[/accent]"
            ).row();

            table.add(
                "Boss hiện tại: "
                    + (selectedBoss == null
                    ? "None"
                    : selectedBoss.localizedName)
            ).row();

            for (UnitType boss : BOSSES) {

                if (boss == null) continue;

                TextButton button =
                    table.button(
                        boss.localizedName,
                        () -> {

                            selectedBoss = boss;

                            Vars.ui.showInfoToast(
                                "Đã chọn: "
                                    + boss.localizedName,
                                2f
                            );

                            dialog.hide();

                            showBossSelector();
                        }
                    ).get();

                button.getLabel().setFontScale(1f);
            }

        }).grow();

        dialog.cont.row();

        dialog.cont.button(
            "Đóng",
            dialog::hide
        ).size(160f, 55f);

        dialog.addCloseButton();

        dialog.show();
    }

    /**
     * Chuyển người chơi thành boss đã chọn.
     */
    private static void applySelectedBoss() {

        if (selectedBoss == null) {
            selectedBoss = DEFAULT_BOSS;
        }

        if (Vars.player == null) return;

        if (Vars.player.dead()) {
            Timer.schedule(
                BossController::applySelectedBoss,
                0.5f
            );

            return;
        }

        Unit oldUnit = Vars.player.unit();

        if (oldUnit == null) return;

        Team team = Vars.player.team();

        if (team == null) {
            team = Team.sharded;
        }

        UnitType boss = selectedBoss;

        /*
         * Ép boss có khả năng xây.
         *
         * Trong v159.7:
         * buildSpeed < 0 => không xây được.
         */
        enableBuilding(boss);

        /*
         * Tạo unit boss.
         */
        Unit newUnit = boss.create(team);

        if (newUnit == null) return;

        /*
         * Đặt boss tại vị trí hiện tại
         * của player.
         */
        newUnit.set(
            oldUnit.x,
            oldUnit.y
        );

        newUnit.rotation = oldUnit.rotation;

        /*
         * Đưa boss vào world.
         */
        newUnit.add();

        /*
         * Gán boss cho player.
         *
         * Player.unit(Unit) là accessor của
         * entity Player trong bản 159.7.
         */
        Vars.player.unit(newUnit);

        /*
         * Xóa unit cũ.
         */
        if (oldUnit.isAdded()) {
            oldUnit.remove();
        }

        /*
         * Thông báo.
         */
        Vars.ui.showInfoToast(
            "Bạn đang điều khiển "
                + boss.localizedName,
            3f
        );
    }

    /**
     * Cho UnitType có khả năng xây dựng.
     */
    private static void enableBuilding(UnitType type) {

        if (type == null) return;

        /*
         * Chỉ lưu giá trị cũ một lần.
         */
        if (!oldBuildSpeed.containsKey(type)) {
            oldBuildSpeed.put(
                type,
                type.buildSpeed
            );
        }

        /*
         * Build speed > 0
         * => unit có khả năng xây.
         */
        type.buildSpeed = 1.0f;

        /*
         * Khoảng cách xây.
         */
        type.buildRange = 80f;
    }

    /**
     * Cho phép đổi boss trong game
     * bằng phím B.
     */
    private static void createInGameButton() {

        if (Vars.ui == null) return;

        Vars.ui.hudGroup.fill(
            parent -> {

                parent.button(
                    "Boss",
                    Icon.units,
                    BossController::showBossSelector
                ).width(100f).height(45f);

            }
        );
    }
}
