package com.example.databaseapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // データベース名を定数に登録
    private final static String DB_NAME = "testDB.db";
    // テーブル名を登録
    private final static String DB_TABLE = "testTable";
    private final static String DB_TABLE_BUYLIST = "buyList";
    // データベースのバージョンを登録
    private final static int DB_VERSION = 1;

    // データベース用のオブジェクトを格納するフィールド変数
    private SQLiteDatabase databaseObject;				//①

    //編集用ショッピングリスト
    private  ArrayList<EditText> editShippingList = new ArrayList<>();
    //ショッピングリスト
    private ArrayList<String> shippingList = new ArrayList<>();
    //読み込みショッピングリストマップ
    Map<String,String> readShoppingListMap = new HashMap<>();
    //ショッピングリスト最大数
    private int SHOPPINGLIST_MAX = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // データベースオブジェクトの作成
        DatabaseHelper dbHelperObject =
                new DatabaseHelper(MainActivity.this);
        databaseObject =
                dbHelperObject.getWritableDatabase();

        //ショッピングリスト表示
        try {
            //DBから読み込みショッピングリストマップ取得
            readShoppingListMap = readToDB();

            //画面表示
            for (int i = 0; i < SHOPPINGLIST_MAX; i++) {
                //viewのmemoのidを取得
                String resViewName = "memo" + i;
                int viewId = getResources().getIdentifier(resViewName, "id", getPackageName());

                //編集用ショッピングリストを取得し表示
                EditText editShipping = (EditText) findViewById(viewId);
                editShipping.setText(readShoppingListMap.get(String.valueOf(i)));
                editShippingList.add(editShipping);
            }
        } catch (Exception e) {
            showDialog(
                    getApplicationContext(),
                    "ERROR",
                    "データの読み込みに失敗しました ");
        }
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();

        //TODO textviewを閉じるときに更新できないことがある
        //viewから値を取得
        for(int i = 0; i < SHOPPINGLIST_MAX; i++){
            //viewのmemoのidを取得
            String resViewName = "memo" + i;
            int viewId =  getResources().getIdentifier(resViewName, "id", getPackageName());

            //ショッピングリストに格納
            shippingList.add(((EditText)findViewById(viewId)).getText().toString());
        }

        try {
            // データベースへの書き込み
            writeToDB(shippingList);
        } catch (Exception e) {
            // 書き込み失敗時にメッセージを表示
            showDialog(
                    MainActivity.this,
                    "ERROR",
                    "データの書き込みに失敗しました "
            );
        }
    }

    /**********************************************
     * データベースへの書き込みを行うメソッド
     * @param shippingList  ショッピングリスト
     **********************************************/
    private void writeToDB(ArrayList<String> shippingList) throws Exception {

        //現在時刻を取得
        Calendar cal = Calendar.getInstance();
        //日付のフォーマットを指定
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        //idを指定してレコードを更新
        for (int i = 0; i < SHOPPINGLIST_MAX; i++) {
            // インスタンスを生成
            ContentValues contentValObject = new ContentValues();
            //カラムの追加
            // 買いたいもの
            contentValObject.put("buy", shippingList.get(i));
            // 店舗
            contentValObject.put("shop", "01");    //⑦
            //最終プッシュ店舗
            contentValObject.put("lastShop", "02");
            // 登録日時
            contentValObject.put("entDate", sdf.format(cal.getTime()));
            //最終更新日時
            contentValObject.put("updDate", "2019-06-10 17:49:30");

            //whereに指定する値 id
            String[] whereValue = {String.valueOf(i)};

            // レコードを上書き
            int numberOfColumns =
                    databaseObject.update(
                            DB_TABLE_BUYLIST,
                            contentValObject,
                            "id = ?",
                            whereValue
                    );
            // レコードが存在しない場合は新規に作成
            if (0 == numberOfColumns) {
                // idを指定
                contentValObject.put("id", i);
                databaseObject.insert(
                        DB_TABLE_BUYLIST,
                        null,
                        contentValObject
                );
            }
            System.out.println("画面入力" + i + "番目登録：" + shippingList.get(i));
        }
    }

    /**********************************************
     * データベースから読み込みを行うメソッド
     * @return  readShoppingListMap  読み込みショッピングリストマップ
     **********************************************/
    private Map<String,String> readToDB() throws Exception {

        //読み込みショッピングリストマップ
        Map<String,String> readShoppingListMap = new HashMap<>();

        // データベースからテーブルを読み込む
        Cursor cursor = databaseObject.query(
                DB_TABLE_BUYLIST,
                new String[]{"id", "buy", "shop", "lastShop", "entDate", "updDate"},
                null,
                null,
                null,
                null,
                "id"
        );

        // cursor内のレコード数が0の場合は例外処理を行うインスタンスを生成
        if (cursor.getCount() == 0) {
            throw new Exception();
        }

        // カーソルの位置を先頭のレコードに移動
        cursor.moveToFirst();
        for(int i = 0; i < cursor.getCount(); i++) {
            // cursorオブジェクト内のレコードのデータをString型に変換
            readShoppingListMap.put(String.valueOf(i),cursor.getString(1));
            cursor.moveToNext();
        }
        // カーソルを閉じる
        cursor.close();
        // レコードのデータを呼び出し元に返す
        return readShoppingListMap;
    }

    /**********************************************
     * ダイアログを表示するメソッド
     * @param context    アプリケーションのContextオブジェクト
     * @param title      ダイアログのタイトル
     * @param text       ダイアログのメッセージ
     **********************************************/
    private static void showDialog(
            Context context,
            String title,
            String text
    ) {
        AlertDialog.Builder varAlertDialog =
                new AlertDialog.Builder(context);
        varAlertDialog.setTitle(title);
        varAlertDialog.setMessage(text);
        varAlertDialog.setPositiveButton("OK", null);
        varAlertDialog.show();
    }

    // ヘルパークラスの定義
    private static class DatabaseHelper extends SQLiteOpenHelper {	//⑮
        // データベースを作成、または開く、管理するための処理
        public DatabaseHelper(Context context) {		//⑯
            // ヘルパークラスクラスのコンストラクターの呼び出し
            super(
                context,
                DB_NAME,
                null,
                DB_VERSION
            );
        }

        // テーブルを作成するメソッドの定義
        @Override
        public void onCreate(SQLiteDatabase db) {	//⑰
            // テーブルの作成
            db.execSQL(			//⑱
                "CREATE TABLE IF NOT EXISTS " +
                        DB_TABLE_BUYLIST +
                "(id text primary key,buy text, shop text, lastShop text, entDate text, updDate text)"
            );
        }

        // データベースをアップグレードするメソッドの定義
        @Override
        public void onUpgrade(		//⑲
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
        ) {
            // 古いバージョンのテーブルが存在する場合はこれを削除
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);	//⑳
            // 新規テーブルの作成
            onCreate(db);
        }
    }





}