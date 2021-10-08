package com.deffe.max.chatgoo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.Utils.HotspotApp;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class AssistantFragment extends Fragment
{
    private final String TAG = AssistantFragment.class.getSimpleName();

    private DatabaseReference botChattingRef;

    private String botId = "61d29dfb150b4378bf63895c95c6c75c";

    private String onlineUserId;

    private View view;

    private static TextToSpeech toSpeech;

    private Activity activity;

    public AssistantFragment()
    {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_assistant, container, false);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        activity = getActivity();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();
        }

        final EditText chatEditText = view.findViewById(R.id.chatting_edittext);
        final ImageView sendImageView = view.findViewById(R.id.send_icon);
        final ImageView recordImageView = view.findViewById(R.id.record_icon);

        RecyclerView botChatRecyclerView = view.findViewById(R.id.bot_chat_recycler_view);

        toSpeech = new TextToSpeech(view.getContext(), new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (toSpeech.getEngines().size() == 0)
                {
                    Toast.makeText(view.getContext(), "There is no Speech engine on your device", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    toSpeech.setLanguage(Locale.US);
                    speak("Hi, what can i do for you");
                }
            }
        });

        botChattingRef = FirebaseDatabase.getInstance().getReference().child("Bot_Messages");

        chatEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                final String text = chatEditText.getText().toString();

                if (text.equals("") || text.equals(" "))
                {
                    recordImageView.setVisibility(View.VISIBLE);
                    sendImageView.setVisibility(View.GONE);
                }
                else
                {
                    recordImageView.setVisibility(View.GONE);
                    sendImageView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        sendImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final String message = chatEditText.getText().toString();

                if (message.equals("") || message.equals(" "))
                {
                    recordImageView.setVisibility(View.VISIBLE);
                    sendImageView.setVisibility(View.GONE);
                }
                else
                {
                    recordImageView.setVisibility(View.GONE);
                    sendImageView.setVisibility(View.VISIBLE);

                    chatEditText.setText("");

                    if (checkDate(view.getContext(),onlineUserId,botId))
                    {
                        storeMessageDetails(view.getContext(),message,onlineUserId,botId);

                        new AssistantResponse(view.getContext(),onlineUserId,botId).execute(message);
                    }
                    else
                    {
                        storeMessageDetails(view.getContext(),message,onlineUserId,botId);

                        new AssistantResponse(view.getContext(),onlineUserId,botId).execute(message);
                    }
                }
            }
        });

        botChatRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(),LinearLayoutManager.VERTICAL,false));
        botChatRecyclerView.setItemAnimator(new DefaultItemAnimator());

        return view;
    }

    private static class AssistantResponse extends AsyncTask<String,Void,Map<String,String>>
    {
        private WeakReference<Context> context;
        private String onlineUserId,botId;

        AssistantResponse(Context context, String onlineUserId, String botId)
        {
            this.context = new WeakReference<>(context);
            this.onlineUserId = onlineUserId;
            this.botId = botId;
        }

        @Override
        protected Map<String,String> doInBackground(String... strings)
        {
            return getText(strings[0]);
        }

        @Override
        protected void onPostExecute(Map<String,String> response)
        {
            super.onPostExecute(response);

            final String resultSpeech = response.get("speech");
            final String resultIntent = response.get("intentName");
            final String resultAny = response.get("any").toLowerCase();
            final String resultDeviceAction = response.get("device-action").toLowerCase();

            if (resultSpeech != null)
            {
                speak(resultSpeech);

                DatabaseReference botChattingRef = FirebaseDatabase.getInstance().getReference().child("Bot_Messages");

                DatabaseReference messageRef = botChattingRef.child(botId).child(onlineUserId).push();

                String messageKey = messageRef.getKey();

                DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
                final String time = df.format(Calendar.getInstance().getTime());

                Map<String,Object> chatMessage = new HashMap<>();

                chatMessage.put("message",resultSpeech);
                chatMessage.put("from",botId);
                chatMessage.put("type","text");
                chatMessage.put("key",messageKey);
                chatMessage.put("time",time);
                chatMessage.put("date",ServerValue.TIMESTAMP);

                if (messageKey != null)
                {
                    botChattingRef.child(botId).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnCompleteListener(new OnCompleteListener<Void>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                switch (resultIntent)
                                {
                                    case "device.settings.anything.check":

                                        if (resultAny.contains("hotspot"))
                                        {
                                            boolean wifiStatus = HotspotApp.isApOn(context.get());

                                            if (wifiStatus)
                                            {
                                                String responseMessage = "Hotspot is enabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "Hotspot is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("wifi") || resultAny.contains("wi-fi"))
                                        {
                                            WifiManager wifi = (WifiManager) context.get().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                                            if (wifi != null && wifi.isWifiEnabled())
                                            {
                                                String responseMessage = "Wifi is enabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "Wifi is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("bluetooth"))
                                        {
                                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                                            if (mBluetoothAdapter == null)
                                            {
                                                String responseMessage = "Your device does not support Bluetooth";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                if (mBluetoothAdapter.isEnabled())
                                                {
                                                    String responseMessage = "Bluetooth is enable";

                                                    speak(responseMessage);

                                                    storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                }
                                                else
                                                {
                                                    String responseMessage = "Bluetooth is disabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                }
                                            }
                                        }
                                        else if (resultAny.contains("gps"))
                                        {
                                            LocationManager manager = (LocationManager) context.get().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                                            if (manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                            {
                                                String responseMessage = "GPS is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "GPS is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("flight mode"))
                                        {
                                            boolean flightMode = Settings.Global.getInt(context.get().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

                                            if (flightMode)
                                            {
                                                String responseMessage = "Flight mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "Flight mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("silent mode"))
                                        {
                                            AudioManager audioManager = (AudioManager) context.get().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 0)
                                            {
                                                String responseMessage = "Silent mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "Silent mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("vibrate mode") || resultAny.contains("vibration mode"))
                                        {
                                            AudioManager audioManager = (AudioManager) context.get().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 1)
                                            {
                                                String responseMessage = resultAny + " is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = resultAny + " is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("normal mode"))
                                        {
                                            AudioManager audioManager = (AudioManager) context.get().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 2)
                                            {
                                                String responseMessage = "Normal mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                            else
                                            {
                                                String responseMessage = "Normal mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                            }
                                        }
                                        else
                                        {
                                            Toast.makeText(context.get(), resultAny, Toast.LENGTH_SHORT).show();
                                        }
                                        break;

                                    case "device.settings.anything.on-off":

                                        if (resultAny.contains("hotspot"))
                                        {
                                            if (resultDeviceAction.equals("on"))
                                            {
                                                boolean wifiStatus = HotspotApp.isApOn(context.get());

                                                if (!wifiStatus)
                                                {
                                                    boolean isEnabled = HotspotApp.configApState(context.get(), "enable");

                                                    if (isEnabled)
                                                    {
                                                        String responseMessage = "Hotspot is enabled";

                                                        speak(responseMessage);

                                                        storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                    }
                                                    else
                                                    {
                                                        String responseMessage = "Error occurred while accessing Hotspot";

                                                        speak(responseMessage);

                                                        storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                    }
                                                }
                                                else
                                                {
                                                    String responseMessage = "Hotspot is already in enabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                }
                                            }
                                            else
                                            {
                                                boolean wifiStatus = HotspotApp.isApOn(context.get());

                                                if (wifiStatus)
                                                {
                                                    boolean isDisabled = HotspotApp.configApState(context.get(), "disable");

                                                    if (isDisabled)
                                                    {
                                                        String responseMessage = "Hotspot is disabled";

                                                        speak(responseMessage);

                                                        storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                    }
                                                    else
                                                    {
                                                        String responseMessage = "Error occurred while accessing Hotspot";

                                                        speak(responseMessage);

                                                        storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                    }
                                                }
                                                else
                                                {
                                                    String responseMessage = "Hotspot is already in disabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(context.get(),onlineUserId,botId,responseMessage);
                                                }
                                            }
                                        }
                                        break;

                                    case "device.apps.open":

                                        if (resultAny.contains("mobile browser"))
                                        {
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));

                                            if (intent.resolveActivity(context.get().getPackageManager()) != null)
                                            {
                                                context.get().startActivity(intent);
                                            }

                                        }
                                        else if (resultAny.contains("google") || resultAny.contains("browser"))
                                        {
                                            WebFragment fragment = new WebFragment();

                                            Bundle bundle = new Bundle();

                                            bundle.putString("url","https://www.google.com");

                                            fragment.setArguments(bundle);

                                            AssistantFragment assistantFragment = new AssistantFragment();

                                            MainActivity

                                            Activity activity = assistantFragment.getActivity();

                                            activity.getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment,fragment).commit();
                                        }
                                        break;
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Toast.makeText(context.get(), "Error while storing documents", Toast.LENGTH_SHORT).show();
                            Log.e("AssistantFragment",e.toString());
                            Crashlytics.log(Log.ERROR,"AssistantFragment",e.getMessage());
                        }
                    });
                }
            }
        }

        private void storeMessageDetails(final Context context, String onlineUserId, String botId, String message)
        {
            DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference().child(botId).child(onlineUserId).push();

            String messageKey = messageRef.getKey();

            DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
            final String time = df.format(Calendar.getInstance().getTime());

            Map<String,Object> chatMessage = new HashMap<>();

            chatMessage.put("message",message);
            chatMessage.put("from", botId);
            chatMessage.put("type","text");
            chatMessage.put("key",messageKey);
            chatMessage.put("time",time);
            chatMessage.put("date",ServerValue.TIMESTAMP);

            if (messageKey != null)
            {
                FirebaseDatabase.getInstance().getReference().child(botId).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Toast.makeText(context, "Error while storing documents", Toast.LENGTH_SHORT).show();
                        Log.e("AssistantFragment",e.toString());
                        Crashlytics.log(Log.ERROR,"AssistantFragment",e.getMessage());
                    }
                });
            }
        }
    }

    private static void speak(String input)
    {
        if (Build.VERSION.SDK_INT >= 21)
        {
            toSpeech.speak(input, TextToSpeech.QUEUE_ADD,null,null);
        }
        else
        {
            toSpeech.speak(input, TextToSpeech.QUEUE_ADD,null);
        }
    }

    public static Map<String,String> getText(String query)
    {
        String text;
        BufferedReader reader = null;

        try
        {
            URL url = new URL("https://api.api.ai/v1/query?v=20150910");

            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Authorization", "Bearer 61d29dfb150b4378bf63895c95c6c75c");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject jsonParam = new JSONObject();

            JSONArray queryArray = new JSONArray();
            queryArray.put(query);

            jsonParam.put("query", queryArray);
            jsonParam.put("lang", "en");
            jsonParam.put("sessionId", "1234567890");


            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            Log.d("AssistantFragment", "after conversion is " + jsonParam.toString());
            wr.write(jsonParam.toString());
            wr.flush();
            Log.d("AssistantFragment", "json is " + jsonParam);

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append("\n");
            }

            text = sb.toString();

            JSONObject queryResult = new JSONObject(text);

            JSONObject retrieveResult = queryResult.getJSONObject("result");

            JSONObject metadata = retrieveResult.getJSONObject("metadata");

            JSONObject parameters = retrieveResult.getJSONObject("parameters");

            JSONObject fulfillment = retrieveResult.getJSONObject("fulfillment");

            Map<String,String> resultValue = new HashMap<>();

            resultValue.put("speech",fulfillment.optString("speech"));
            resultValue.put("resolvedQuery",retrieveResult.optString("resolvedQuery"));
            resultValue.put("intentName",metadata.optString("intentName"));
            resultValue.put("any",parameters.optString("any"));
            resultValue.put("device-action",parameters.optString("device-action"));

            return resultValue;

        } catch (Exception ex)
        {
            Log.d("AssistantFragment", "exception at last " + ex);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignored)
            {
            }
        }

        return null;
    }

    private String getTodayDate()
    {
        DateFormat todayDate = new SimpleDateFormat("d MMM yyyy", Locale.US);

        return todayDate.format(Calendar.getInstance().getTime());
    }

    private String formatToYesterdayOrToday(String date) throws ParseException
    {
        Date dateTime = new SimpleDateFormat("d MMM yyyy", Locale.US).parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateTime);
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR))
        {
            return "Today";
        }
        else if (calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR))
        {
            return "Yesterday";
        }
        else
        {
            return date;
        }
    }

    private void storeDateRef(Context context, String online_key,String bot_key)
    {
        SharedPreferences preferences = context.getSharedPreferences(online_key,MODE_PRIVATE);

        if (preferences != null)
        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(bot_key,getTodayDate());
            editor.apply();
        }
    }

    private String readDateRef(Context context, String online_key, String bot_key)
    {
        String todayDate,checkDate = null;

        SharedPreferences date = context.getSharedPreferences(online_key,MODE_PRIVATE);

        if (date != null)
        {
            checkDate = date.getString(bot_key,null);
        }

        if (date == null)
        {
            todayDate = null;
        }
        else
        {
            todayDate = checkDate;
        }

        return todayDate;
    }

    private boolean checkDate(Context context, String online, String bot)
    {
        String today = readDateRef(context,online,bot);

        boolean result = false;

        if (today == null)
        {
            updateDateRef(context,online,bot);

            result = true;
        }
        else
        {
            try
            {
                String checkDate = formatToYesterdayOrToday(today);

                if (!checkDate.equals("Today"))
                {
                    updateDateRef(context,online,bot);

                    result = true;
                }
                else
                {
                    result = false;
                }
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
        }

        return result;
    }

    private void updateDateRef(final Context context, final String online_key, final String bot_key)
    {
        storeDateRef(context,online_key,bot_key);

        DatabaseReference dateRef = botChattingRef.child(botId).child(onlineUserId).push();

        final String date_push_id = dateRef.getKey();

        final Map<String,Object> messageDate = new HashMap<>();

        messageDate.put("type", "date");
        messageDate.put("today_date", getTodayDate());
        messageDate.put("from",online_key);
        messageDate.put("key",date_push_id);
        messageDate.put("date", ServerValue.TIMESTAMP);

        if (date_push_id != null)
        {
            botChattingRef.child(botId).child(onlineUserId).child(date_push_id).updateChildren(messageDate).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Toast.makeText(context, "Error while storing documents", Toast.LENGTH_SHORT).show();
                    Log.e(TAG,e.toString());
                    Crashlytics.log(Log.ERROR,TAG,e.getMessage());
                }
            });
        }
    }

    private void storeMessageDetails(final Context context, String message, String onlineUserId, String botId)
    {
        DatabaseReference messageRef = botChattingRef.child(botId).child(onlineUserId).push();

        String messageKey = messageRef.getKey();

        DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
        final String time = df.format(Calendar.getInstance().getTime());

        Map<String,Object> chatMessage = new HashMap<>();

        chatMessage.put("message",message);
        chatMessage.put("from",onlineUserId);
        chatMessage.put("type","text");
        chatMessage.put("key",messageKey);
        chatMessage.put("time",time);
        chatMessage.put("date",ServerValue.TIMESTAMP);

        if (messageKey != null)
        {
            botChattingRef.child(botId).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Toast.makeText(context, "Error while storing documents", Toast.LENGTH_SHORT).show();
                    Log.e(TAG,e.toString());
                    Crashlytics.log(Log.ERROR,TAG,e.getMessage());
                }
            });
        }
    }
}
