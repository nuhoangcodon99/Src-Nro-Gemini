/*
 * Beo Sờ tu đi ô
 */
package com.arriety.MaQuaTang;

import com.girlkun.database.GirlkunDB;
import com.girlkun.models.item.Item.ItemOption;
import com.girlkun.models.player.Player;
import com.girlkun.services.NpcService;
import com.girlkun.services.Service;
import com.girlkun.utils.Logger;
import com.girlkun.utils.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author Dev by Ánh Ngọc
 */
public class MaQuaTangManager {

     public String name;
    public final ArrayList<MaQuaTang> listGiftCode = new ArrayList<>();

    private static MaQuaTangManager instance;

    public static MaQuaTangManager gI() {
        if (instance == null) {
            instance = new MaQuaTangManager();
        }
        return instance;
    }

    public void init() {
        try (Connection con = GirlkunDB.getConnection();) {

            PreparedStatement ps = con.prepareStatement("SELECT * FROM giftcode");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MaQuaTang giftcode = new MaQuaTang();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if(giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (int i = 0; i < jar.size(); ++i) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);
                        giftcode.detail.put(Integer.parseInt(jsonObj.get("id").toString()),
                                Integer.parseInt(jsonObj.get("quantity").toString()));
                        jsonObj.clear();
                    }
                }

                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("itemoption"));
                if (option != null) {
                    String param = "-1";
                    for (int u = 0; u < option.size(); u++) {
                        JSONObject jsonobject = (JSONObject) option.get(u);
                        param = jsonobject.get("param").toString();
//                        Logger.log("Done-------------------" + param);
                        if(param == "-1") {
                            param = String.valueOf(Util.nextInt(1, 50));
                        }
                        giftcode.option.add(new ItemOption(Integer.parseInt(jsonobject.get("id").toString()), Integer.parseInt(param)));
                        jsonobject.clear();

                    }

                }
                JSONArray listPlayer = (JSONArray) JSONValue.parse(rs.getString("players_used"));

                if (listPlayer != null) {
                    for (int i = 0; i < listPlayer.size(); ++i) {
                        giftcode.listIdPlayer.add(Integer.parseInt(listPlayer.get(i).toString()));
                    }
                }

                listGiftCode.add(giftcode);
            }
            con.close();
        } catch (Exception erorlog) {
            erorlog.printStackTrace();
        }
    }

    public void sizeList(Player pl) {
        Service.getInstance().sendThongBao(pl, "" + MaQuaTang.class);
    }

    public MaQuaTang checkUseGiftCode(int idPlayer, String code) {
        for (MaQuaTang giftCode : listGiftCode) {
            if (giftCode.code.equals(code) && giftCode.countLeft > 0 && !giftCode.isUsedGiftCode(idPlayer)) {
                giftCode.countLeft -= 1;
                giftCode.addPlayerUsed(idPlayer, giftCode.id);
                return giftCode;
            }
        }
        return null;
    }

    public void updateListUserCode(int idCode, ArrayList<Integer> UserCode) {
        Logger.warning("" + UserCode);
        try {
            GirlkunDB.executeUpdate("update giftcode set players_used = ? where id = ?", "" + UserCode,
                    idCode);
        } catch (Exception e) {
            Logger.warning("" + e);
        }
    }

    public void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (MaQuaTang giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng: ").append(giftCode.countLeft).append("\b")
                    .append(", Ngày tạo: ")
                    .append(giftCode.datecreate).append("Ngày hết hạn").append(giftCode.dateexpired);
        }

        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }
}
