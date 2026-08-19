package bosscontroller;

import arc.Core;
import arc.Events;
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

    /*
     * Boss mặc định.
     */
    private static UnitType selectedBoss = UnitTypes.reign;

    /*
     * Danh sách boss xuất hiện trong GUI.
     *
     * Có thể thêm UnitTypes khác vào đây.
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
     * Dùng để tránh gọi becomeBoss() nhiều lần
     * trong cùng một lần load map.
     */
    private static boolean changingUnit = false;

    /*
     * ==========================================
     * CONSTRUCTOR
     * ==========================================
     */

    public BossController() {

        /*
         * Client đã load xong.
         *
         * Thêm nút Boss Controller vào menu chính.
         */
        Events.on(ClientLoadEvent.class, event -> {

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

            changingUnit = false;

            /*
             * Chờ player/unit được tạo hoàn chỉnh.
             */
            Timer.schedule(() -> {

                becomeBoss();

            }, 0.5f);
        });
    }

    /*
     * ==========================================
     * GUI
     * ==========================================
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
                    + getBossName(selectedBoss)
                    + "[/accent]"
            ).row();

            table.add(
                "Chọn boss trước khi vào map."
            ).row();

            table.add().height(10f).row();

            /*
             * Tạo nút cho từng boss.
             */
            for (UnitType type : BOSS_LIST) {

                if (type == null) continue;

                TextButton button = table.button(
                    type.localizedName,
                    () -> {

                        selectedBoss = type;

                        /*
                         * Đóng GUI.
                         */
                        Core.app.post(dialog::hide);

                        /*
                         * Thông báo lựa chọn.
                         */
                        Vars.ui.showInfoToast(
                            "Đã chọn: "
                                + type.localizedName,
                            2f
                        );
                    }
                ).get();

                button.getLabel().setFontScale(1f);

                table.row();
            }

        }).grow();

        dialog.buttons.button(
            "Đóng",
            dialog::hide
        );

        dialog.show();
    }

    /*
     * ==========================================
     * ĐỔI PLAYER THÀNH BOSS
     * ==========================================
     */

    private static void becomeBoss() {

        /*
         * Không có player thì thử lại.
         */
        if (Vars.player == null) {
            retryBecomeBoss();
            return;
        }

        /*
         * Player chưa có unit.
         */
        Unit oldUnit = Vars.player.unit();

        if (oldUnit == null) {
            retryBecomeBoss();
            return;
        }

        /*
         * Nếu đang trong quá trình đổi unit,
         * không thực hiện lần nữa.
         */
        if (changingUnit) {
            return;
        }

        /*
         * Boss mặc định.
         */
        if (selectedBoss == null) {
            selectedBoss = UnitTypes.reign;
        }

        /*
         * Đã là boss được chọn rồi.
         */
        if (oldUnit.type == selectedBoss) {

            prepareBoss(selectedBoss);

            return;
        }

        changingUnit = true;

        /*
         * Lưu thông tin unit cũ.
         */
        float x = oldUnit.x;
        float y = oldUnit.y;
        float rotation = oldUnit.rotation;

        Team team = Vars.player.team();

        /*
         * Tạo unit boss.
         *
         * UnitType.create(Team) là API của UnitType.
         */
        Unit boss = selectedBoss.create(team);

        if (boss == null) {
            changingUnit = false;
            return;
        }

        /*
         * Cho boss kế thừa vị trí.
         */
        boss.set(x, y);

        boss.rotation = rotation;

        /*
         * Chuẩn bị khả năng xây.
         */
        prepareBoss(selectedBoss);

        /*
         * Thêm boss vào world.
         */
        boss.add();

        /*
         * Gán boss cho player.
         *
         * Đây là phần quan trọng nhất:
         * player sẽ điều khiển unit mới.
         */
        Vars.player.unit(boss);

        /*
         * Xóa unit cũ.
         */
        if (oldUnit.isAdded()) {
            oldUnit.remove();
        }

        changingUnit = false;

        /*
         * Thông báo.
         */
        Vars.ui.showInfoToast(
            "Bạn đang điều khiển: "
                + selectedBoss.localizedName,
            3f
        );
    }

    /*
     * ==========================================
     * RETRY
     * ==========================================
     */

    private static void retryBecomeBoss() {

        Timer.schedule(() -> {

            if (Vars.player == null) {
                retryBecomeBoss();
                return;
            }

            if (Vars.player.unit() == null) {
                retryBecomeBoss();
                return;
            }

            becomeBoss();

        }, 0.5f);
    }

    /*
     * ==========================================
     * CHUẨN BỊ BOSS XÂY DỰNG
     * ==========================================
     */

    private static void prepareBoss(UnitType type) {

        if (type == null) return;

        /*
         * Một số boss vốn đã có buildSpeed.
         *
         * Với những UnitType không xây được,
         * đặt buildSpeed > 0 để mở khả năng xây.
         */
        if (type.buildSpeed <= 0f) {
            type.buildSpeed = 1f;
        }

        /*
         * Một số boss có buildRange quá nhỏ
         * hoặc không có khoảng cách xây.
         */
        if (type.buildRange <= 0f) {
            type.buildRange = 80f;
        }
    }

    /*
     * ==========================================
     * TÊN BOSS
     * ==========================================
     */

    private static String getBossName(UnitType type) {

        if (type == null) {
            return "Reign";
        }

        return type.localizedName;
    }

    /*
     * ==========================================
     * GETTER
     * ==========================================
     */

    public static UnitType selectedBoss() {
        return selectedBoss;
    }
}
