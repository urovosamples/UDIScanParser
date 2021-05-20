package com.ubx.example.udiscan;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getName();

    TextView sym_nametv;
    private TextView mBarcodeResult;
    private ScanManager mScanManager;
    int[] idbuf = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
    String[] action_value_buf = new String[]{ScanManager.ACTION_DECODE, ScanManager.BARCODE_STRING_TAG};
    int[] udiConfigbuf = new int[]{PropertyID.SEND_TOKENS_OPTION, PropertyID.SEND_TOKENS_FORMAT, PropertyID.SEND_TOKENS_SEPARATOR, PropertyID.ENABLE_PARSER_UDICODE};
    int[] udiConfigval = new int[4];
    private static int UDI_GS1 = 0x0002;
    private static int UDI_HIBCC = 0x0004;
    private static int UDI_ICCBBA = 0x0008;
    private static int UDI_MA = 0x0010;
    private static int UDI_AHM = 0x0020;
    private int mParserUDICodes = UDI_GS1 | UDI_HIBCC | UDI_ICCBBA | UDI_MA | UDI_AHM;
    private class AsyncUDIDataUpdate extends AsyncTask<String, Void, String> {

        private ArrayList<TableRow> rows;
        AsyncUDIDataUpdate(ArrayList<TableRow> rows){
            this.rows = rows;
        }

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String decodeType) {
            //sym_nametv.setText(decodeType);
            TableLayout tl = (TableLayout) findViewById(R.id.tableView);

            tl.removeAllViews();
            for (TableRow row : rows) {
                tl.addView(row);
            }
        }
    }
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            mBarcodeResult.setText("");
            sym_nametv.setText("");
            String[] datas = intent.getStringArrayExtra(action_value_buf[1]+"Lists");
            if(datas != null) {
                for(int i =0; i < datas.length; i++) {
                    mBarcodeResult.append(datas[i]);
                    mBarcodeResult.append("\n");
                }
            } else {
                byte[] barcodeData = intent.getByteArrayExtra("barcode");
                int dataLength = intent.getIntExtra("length", 0);
                sym_nametv.setText("Data:\n" + (new String(barcodeData, 0, dataLength)));
                String result = intent.getStringExtra(action_value_buf[1]);
                boolean UDIMode = intent.getBooleanExtra("UDIMode", false);
                if(UDIMode) {
                    int sendTokensFormat = intent.getIntExtra("sendTokensFormat", 0);
                    if(sendTokensFormat == 1 || sendTokensFormat == 2) {
                        ArrayList<TableRow> rows = new ArrayList<TableRow>();

                        // Adding header row
                        TableRow row= new TableRow(MainActivity.this);
                        row.setBackgroundColor(Color.BLACK);
                        row.setPadding(1, 1, 1, 1);

                        TableRow.LayoutParams llp = new TableRow.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,TableLayout.LayoutParams.MATCH_PARENT);
                        llp.setMargins(0, 0, 2, 0);

                        TextView keyText = new TextView(MainActivity.this);
                        keyText.setPadding(5, 5, 5, 5);
                        keyText.setLayoutParams(llp);
                        keyText.setBackgroundColor(Color.WHITE);
                        keyText.setText("Key");
                        row.addView(keyText);

                        TextView valueText = new TextView(MainActivity.this);
                        valueText.setPadding(5, 5, 5, 5);
                        valueText.setBackgroundColor(Color.WHITE);
                        valueText.setText("Value");
                        row.addView(valueText);

                        rows.add(row);
                        if(sendTokensFormat == 2) {
                            try {
                                // 返回json的数组
                                if(udiConfigval[0] == 2) {
                                    Log.d(TAG, result);
                                    //int index = result.indexOf("[");
                                    //result = result.substring(index);
                                    result = result.substring(dataLength);
                                    mBarcodeResult.setText("JSON:\n" + result);
                                    Log.d(TAG, result);
                                } else {
                                    mBarcodeResult.setText("JSON:\n" + result);
                                }
                                JSONArray jsonArray = new JSONArray(result);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONArray jsonObject2 = jsonArray.getJSONArray(i);
                                    if(jsonObject2.length() == 2) {
                                        String mKey = (String)jsonObject2.get(0);
                                        String mValue = (String)jsonObject2.get(1);
                                        //Log.d("UDI", "" + jsonObject2.get(1) + " " + jsonObject2.get(1));
                                        row= new TableRow(MainActivity.this);
                                        row.setBackgroundColor(Color.BLACK);
                                        row.setPadding(1, 1, 1, 1);

                                        keyText = new TextView(MainActivity.this);
                                        keyText.setPadding(5, 5, 5, 5);
                                        keyText.setLayoutParams(llp);
                                        keyText.setBackgroundColor(Color.WHITE);
                                        keyText.setText(""+mKey);
                                        row.addView(keyText);

                                        valueText = new TextView(MainActivity.this);
                                        valueText.setPadding(5, 5, 5, 5);
                                        valueText.setBackgroundColor(Color.WHITE);
                                        valueText.setLayoutParams(llp);
                                        valueText.setText(""+mValue);
                                        row.addView(valueText);

                                        rows.add(row);
                                    }
                                }
                            } catch (Exception e) {
                                // TODO: handle exception
                                e.printStackTrace();
                            }
                        } else {
                            if(udiConfigval[0] == 2) {
                                Log.d(TAG, result);
                                if(udiConfigval[2] > 0) {//remove SEPARATOR
                                    int index = result.indexOf("<");
                                    result = result.substring(index);
                                } else {
                                    result = result.substring(dataLength);
                                }

                                mBarcodeResult.setText("XML:\n" + result);
                                Log.d(TAG, result);
                            } else {
                                mBarcodeResult.setText("XML:\n" + result);
                            }
                            InputStream inputStream = null;
                            try {
                                inputStream = new ByteArrayInputStream(result.getBytes());
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                XmlPullParser parser = factory.newPullParser();
                                parser.setInput(inputStream, "utf-8");

                                int eventType = parser.getEventType();

                                while (eventType != XmlPullParser.END_DOCUMENT) {
                                    switch (eventType) {
                                        case XmlPullParser.START_DOCUMENT:
                                            break;
                                        case XmlPullParser.START_TAG:
                                            if (!"UDI".equals(parser.getName())) {
                                                row= new TableRow(MainActivity.this);
                                                row.setBackgroundColor(Color.BLACK);
                                                row.setPadding(1, 1, 1, 1);

                                                keyText = new TextView(MainActivity.this);
                                                keyText.setPadding(5, 5, 5, 5);
                                                keyText.setLayoutParams(llp);
                                                keyText.setBackgroundColor(Color.WHITE);
                                                keyText.setText(""+parser.getName());
                                                row.addView(keyText);

                                                valueText = new TextView(MainActivity.this);
                                                valueText.setPadding(5, 5, 5, 5);
                                                valueText.setBackgroundColor(Color.WHITE);
                                                valueText.setLayoutParams(llp);
                                                valueText.setText(""+parser.nextText());
                                                row.addView(valueText);

                                                rows.add(row);
                                            }
                                            break;
                                        case XmlPullParser.END_TAG:
                                            Log.d(TAG, "END_TAG = " + parser.getName());
                                            break;

                                    }
                                    eventType = parser.next();
                                }
                            } catch (XmlPullParserException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (NumberFormatException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } finally {
                                try {
                                    if (inputStream != null) {
                                        inputStream.close();
                                    }
                                } catch (IOException e) {
                                }
                            }
                        }
                        new AsyncUDIDataUpdate(rows).execute(result);
                    } else {

                    }
                }
            }
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(mScanManager != null) {
            mScanManager.stopDecode();
        }
        unregisterReceiver(mScanReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mScanManager = new ScanManager();
        action_value_buf = mScanManager.getParameterString(idbuf);
        IntentFilter filter = new IntentFilter();
        filter.addAction(action_value_buf[0]);
        registerReceiver(mScanReceiver, filter);
        int[] id = new int[]{PropertyID.WEDGE_KEYBOARD_ENABLE,PropertyID.SEND_GOOD_READ_BEEP_ENABLE};
        int[] val = mScanManager.getParameterInts(id);
        if(val[0] == 1) {
            val[0] = 0;
            val[1] = 1;
            mScanManager.setPropertyInts(id, val);
        }
        udiConfigval = mScanManager.getParameterInts(udiConfigbuf);
        udiConfigval[0] = 1;//0 disable udi parse, 1 only output parse udi data, 2 output barcode and parse udi data
        udiConfigval[1] = 1;//0 string , 1 xml format, 2 json format
        udiConfigval[2] = 1;//input a Separator char when output barcode and parse udi data; 0 none, 1 \n, 2 \n, 3 tab
        udiConfigval[3] = mParserUDICodes;
        mScanManager.setParameterInts(udiConfigbuf, udiConfigval);
        sym_nametv = (TextView) findViewById(R.id.sym_name);
        mBarcodeResult = (TextView) findViewById(R.id.barcode_result);
    }
}