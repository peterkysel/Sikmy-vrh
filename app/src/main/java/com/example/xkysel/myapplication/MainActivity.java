package com.example.xkysel.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xkysel.myapplication.AdapterScreen.TableActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private EditText _angle_editText;
    private EditText _speed_editText;
    private Switch _switch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _angle_editText = (EditText) findViewById(R.id.ET_angle);
        _speed_editText = (EditText) findViewById(R.id.ET_speed);
        _switch = (Switch) findViewById(R.id.main_switch);

        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    onClickStartButton();
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void onClickStartButton() throws IOException, ExecutionException, InterruptedException {
        int angle = Integer.valueOf(_angle_editText.getText().toString());
        int speed = Integer.valueOf(_speed_editText.getText().toString());
        TrajectoryProjectile projectile = new TrajectoryProjectile(angle, speed);

        Intent intent = new Intent(this, TableActivity.class);

        if (angle > 89) {
            Toast.makeText(this, "Uhol nemoze byt vacsi ako 89 stupnov !", Toast.LENGTH_SHORT).show();
        } else {
            if (!_switch.isChecked()) {
                projectile.calculateDistanceTraveled();
                projectile.calculateTimeOfFlight();
                projectile.calculate_X_Y_axis();
            } else {
                String _ip = "192.168.137.1";
                String _port = "8080";
                String link = "http://" + _ip + ":" + _port + "/";
                Download asyncTask_get_XY = download_XYaxis(link, angle, speed);

                while (!asyncTask_get_XY.get().equals("FINISHED") &&
                        !asyncTask_get_XY.get().equals("Error"));

                if (asyncTask_get_XY.get().equals("Error")) {
                    Toast.makeText(getBaseContext(), "Chyba pri pripojeni sa na server !", Toast.LENGTH_SHORT).show();
                    return;
                }

                Download asyncTask_get_ListOfTimes = download_getListOfTimes(link);
                Download asyncTask_get_DistanceTraveled = download_distanceTraveled(link);
                Download asyncTask_get_TimeOfFlight = download_getTimeOfFlight(link);
                Download asyncTask_get_HighestHeight = download_getHighestHeight(link);

                while ( !asyncTask_get_ListOfTimes.get().equals("FINISHED") ||
                        !asyncTask_get_DistanceTraveled.get().equals("FINISHED") ||
                        !asyncTask_get_TimeOfFlight.get().equals("FINISHED") ||
                        !asyncTask_get_HighestHeight.get().equals("FINISHED"));

                projectile.set_listOfX(asyncTask_get_XY.get_xAxis());
                projectile.set_listOfY(asyncTask_get_XY.get_yAxis());
                projectile.set_listOfTimes(asyncTask_get_ListOfTimes.get_listOfTimes());
                projectile.set_distanceTraveled(asyncTask_get_DistanceTraveled.get_distanceTraveled());
                projectile.set_timeOfFlight(asyncTask_get_TimeOfFlight.get_timeOfFlight());
                projectile.set_highestHeight(asyncTask_get_HighestHeight.get_highestHeight());
            }
            intent.putExtra("Projectile", projectile);
            startActivity(intent);
        }
    }

    private Download download_XYaxis(String link, int angle, int speed) {
        String getRequest = "getXY/angle=" + String.valueOf(angle) + "&speed="+ String.valueOf(speed);
        link = link + getRequest;
        Download download = new Download("getXY");
        download.execute(link);

        return download;
    }

    private Download download_getListOfTimes(String link) {
        String getRequest = "getListOfTimes";
        link = link + getRequest;
        Download download = new Download("getListOfTimes");
        download.execute(link);

        return download;
    }

    private Download download_distanceTraveled(String link) {
        String getRequest = "getDistanceTraveled";
        link = link + getRequest;
        Download download = new Download("getDistanceTraveled");
        download.execute(link);

        return download;
    }

    private Download download_getTimeOfFlight(String link) {
        String getRequest = "getTimeOfFlight";
        link = link + getRequest;
        Download download = new Download("getTimeOfFlight");
        download.execute(link);

        return download;
    }

    private Download download_getHighestHeight(String link) {
        String getRequest = "getHighestHeight";
        link = link + getRequest;
        Download download = new Download("getHighestHeight");
        download.execute(link);

        return download;
    }

    private class Download extends AsyncTask<String, Void, String> {
        private ArrayList<Double> _listOfTimes = new ArrayList<>();
        private ArrayList<Double> _xAxis;
        private ArrayList<Double> _yAxis;

        private String _downloadGoal;
        private Double _distanceTraveled;
        private Double _timeOfFlight;
        private Double _highestHeight;


        Download(String downloadGoal) {
            this._downloadGoal = downloadGoal;
        }

        @Override
        protected String doInBackground(String... strings) {
            String link = strings[0];
            HttpURLConnection conn;
            URL url;
            try {
                url = new URL(link);
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String jsonString = "",data;

                while ((data = reader.readLine()) != null){
                    jsonString += data + "\n";
                }

                switch (_downloadGoal) {
                    case "getXY":
                        process_XY(jsonString);
                        break;
                    case "getListOfTimes":
                        process_listOfTimes(jsonString);
                        break;
                    case "getDistanceTraveled":
                        process_distanceTraveled(jsonString);
                        break;
                    case "getTimeOfFlight":
                        process_timeOfFlight(jsonString);
                        break;
                    case "getHighestHeight":
                        process_highestHeight(jsonString);
                        break;
                    default:
                        return "Neznamy ciel stahovania !";
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return "Error";
            }
            return "FINISHED";
        }

        private boolean process_XY(String jsonString) throws JSONException {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray xAxisJSON = jsonObject.getJSONArray("0");
            JSONArray yAxisJSON = jsonObject.getJSONArray("1");

            _xAxis = new ArrayList<>();
            _yAxis = new ArrayList<>();

            for (int i = 0; i < xAxisJSON.length(); i++) {
                _xAxis.add(Double.valueOf(xAxisJSON.get(i).toString()));
                _yAxis.add(Double.valueOf(yAxisJSON.get(i).toString()));
            }
            return true;
        }

        private boolean process_listOfTimes(String jsonString) throws JSONException {
            jsonString = jsonString.replace("[", "").replace("]", "");
            String[] splitJsonString = jsonString.split(",");

            _listOfTimes = new ArrayList<>();

            for (String aSplitJsonString : splitJsonString) {
                _listOfTimes.add(Double.valueOf(aSplitJsonString));
            }
            return true;
        }

        private boolean process_distanceTraveled(String jsonString) throws JSONException {
            this._distanceTraveled = Double.valueOf(jsonString);

            return true;
        }

        private boolean process_timeOfFlight(String jsonString) throws JSONException {
            this._timeOfFlight = Double.valueOf(jsonString);

            return true;
        }

        private boolean process_highestHeight(String jsonString) throws JSONException {
            this._highestHeight = Double.valueOf(jsonString);

            return true;
        }

        private ArrayList<Double> get_xAxis() {
            return _xAxis;
        }

        private ArrayList<Double> get_yAxis() {
            return _yAxis;
        }

        private ArrayList<Double> get_listOfTimes() {
            return _listOfTimes;
        }

        private Double get_distanceTraveled() {
            return _distanceTraveled;
        }

        private Double get_timeOfFlight() {
            return _timeOfFlight;
        }

        private Double get_highestHeight() {
            return _highestHeight;
        }
    }
}
