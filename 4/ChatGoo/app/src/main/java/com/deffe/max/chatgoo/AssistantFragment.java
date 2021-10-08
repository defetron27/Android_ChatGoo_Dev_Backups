package com.deffe.max.chatgoo;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.deffe.max.chatgoo.Adapters.BotChattingRecyclerAdapter;
import com.deffe.max.chatgoo.Models.MessageTypesModel;
import com.deffe.max.chatgoo.Utils.HotspotApp;
import com.deffe.max.chatgoo.Utils.NetworkStatus;
import com.deffe.max.chatgoo.Utils.PermissionUtil;
import com.deffe.max.chatgoo.Views.AVLoadingIndicatorView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class AssistantFragment extends Fragment implements RecognitionListener,AIListener
{
    private final String TAG = AssistantFragment.class.getSimpleName();

    private static final String KEYPHRASE = "ok ";
    private static final String KWS_SEARCH = "wakeup";

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private DatabaseReference botChattingRef;

    private String botId = "61d29dfb150b4378bf63895c95c6c75c";

    private String onlineUserId;

    private View view;

    private ArrayList<MessageTypesModel> chatMessages = new ArrayList<>();

    private BotChattingRecyclerAdapter adapter;

    private AVLoadingIndicatorView loadingIndicatorView;

    private EditText chatEditText;
    private ImageView sendImageView;
    private ImageView recordImageView;

    private PermissionUtil permissionUtil;

    private TextToSpeech toSpeech;

    private SpeechRecognizer recognizer;

    private AIService aiService;

    private Activity activity;

    private String resultAny,resultDeviceAction,resultNumber,resultMail,resultMessageAction,resultContactName;

    public AssistantFragment()
    {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        view = inflater.inflate(R.layout.fragment_assistant, container, false);

        activity = getActivity();

        permissionUtil = new PermissionUtil(view.getContext());

        if (checkPermission(PermissionUtil.READ_RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.RECORD_AUDIO))
            {
                showPermissionExplanation(PermissionUtil.READ_RECORD_AUDIO);
            }
            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_RECORD_AUDIO))
            {
                requestPermission(PermissionUtil.READ_RECORD_AUDIO);
                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_RECORD_AUDIO);
            }
            else
            {
                Toast.makeText(view.getContext(), "Please allow record audio permission in your app settings", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                intent.setData(uri);
                this.startActivity(intent);
            }
        }
        if (checkPermission(PermissionUtil.READ_INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.INTERNET))
            {
                showPermissionExplanation(PermissionUtil.READ_INTERNET);
            }
            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_INTERNET))
            {
                requestPermission(PermissionUtil.READ_INTERNET);
                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_INTERNET);
            }
            else
            {
                Toast.makeText(view.getContext(), "Please allow internet permission in your app settings", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                intent.setData(uri);
                this.startActivity(intent);
            }
        }
        if (checkPermission(PermissionUtil.READ_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                showPermissionExplanation(PermissionUtil.READ_WRITE_EXTERNAL_STORAGE);
            }
            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_WRITE_EXTERNAL_STORAGE))
            {
                requestPermission(PermissionUtil.READ_WRITE_EXTERNAL_STORAGE);
                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_WRITE_EXTERNAL_STORAGE);
            }
            else
            {
                Toast.makeText(view.getContext(), "Please allow write storage permission in your app settings", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                intent.setData(uri);
                this.startActivity(intent);
            }
        }
        else
        {
            new SetupTask(this).execute();
        }

        AIConfiguration config = new AIConfiguration("61d29dfb150b4378bf63895c95c6c75c" ,AIConfiguration.SupportedLanguages.English,AIConfiguration.RecognitionEngine.System);

        aiService = AIService.getService(view.getContext(), config);
        aiService.setListener(this);

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
                }
            }
        });

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            onlineUserId = firebaseUser.getUid();
        }

        chatEditText = view.findViewById(R.id.chatting_edittext);
        sendImageView = view.findViewById(R.id.send_icon);
        recordImageView = view.findViewById(R.id.record_icon);
        loadingIndicatorView = view.findViewById(R.id.speech_indicator);

        RecyclerView botChatRecyclerView = view.findViewById(R.id.bot_chat_recycler_view);
        botChatRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        botChatRecyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new BotChattingRecyclerAdapter(view.getContext(),chatMessages);
        botChatRecyclerView.setAdapter(adapter);

        botChattingRef = FirebaseDatabase.getInstance().getReference().child("Bot_Messages");

        ValueEventListener eventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                chatMessages.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    MessageTypesModel model = snapshot.getValue(MessageTypesModel.class);

                    chatMessages.add(model);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        };

        botChattingRef.child(botId).child(onlineUserId).addValueEventListener(eventListener);

        chatEditText.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (chatEditText.isEnabled())
                {
                    final String text = chatEditText.getText().toString();

                    if (text.equals("") || text.equals(" "))
                    {
                        chatEditText.setText("");

                        aiService.stopListening();

                        loadingIndicatorView.setVisibility(View.GONE);

                        recordImageView.setVisibility(View.VISIBLE);

                        switchSearch(KWS_SEARCH);
                    }
                    else
                    {
                        chatEditText.setClickable(false);

                        aiService.stopListening();

                        loadingIndicatorView.setVisibility(View.GONE);

                        recordImageView.setVisibility(View.GONE);

                        sendImageView.setVisibility(View.VISIBLE);

                        switchSearch(KWS_SEARCH);
                    }
                }
                else
                {
                    chatEditText.setEnabled(true);

                    chatEditText.setText("");

                    aiService.stopListening();

                    loadingIndicatorView.setVisibility(View.GONE);

                    recordImageView.setVisibility(View.VISIBLE);

                    switchSearch(KWS_SEARCH);
                }
            }
        });

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

                if (text.equals("") || text.equals(" ")) {
                    recordImageView.setVisibility(View.VISIBLE);
                    sendImageView.setVisibility(View.GONE);
                } else {
                    recordImageView.setVisibility(View.GONE);
                    sendImageView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        recordImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (recognizer != null)
                {
                    if (NetworkStatus.isConnected(view.getContext()) && NetworkStatus.isConnectedFast(view.getContext()))
                    {
                        startToRecognizeText();
                    }
                    else
                    {
                        Toast.makeText(activity, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        sendImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (NetworkStatus.isConnected(view.getContext()) && NetworkStatus.isConnectedFast(view.getContext()))
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

                        if (checkDate(view.getContext(), onlineUserId, botId))
                        {
                            storeMessageDetails(view.getContext(), onlineUserId, botId, message);

                            new AssistantResponse(AssistantFragment.this).execute(message);
                        }
                        else
                        {
                            storeMessageDetails(view.getContext(), onlineUserId, botId, message);

                            new AssistantResponse(AssistantFragment.this).execute(message);
                        }
                    }
                }
                else
                {
                    Toast.makeText(activity, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    //AI SERVICE RESPONSE RESULT

    @Override
    public void onResult(AIResponse result)
    {
        final Result res = result.getResult();

        if (res != null)
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    final String speech = res.getFulfillment().getSpeech();
                    final String resolvedQuery = res.getResolvedQuery();

                    if (checkDate(view.getContext(), onlineUserId, botId))
                    {
                        storeMessageDetails(view.getContext(), onlineUserId, botId, resolvedQuery);
                    }
                    else
                    {
                        storeMessageDetails(view.getContext(), onlineUserId, botId, resolvedQuery);
                    }

                    Map<String,String> resultValue = new HashMap<>();

                    resultValue.put("speech",speech);
                    resultValue.put("intentName",res.getMetadata().getIntentName());

                    if (res.getParameters() != null && !res.getParameters().isEmpty())
                    {
                        for (Map.Entry<String,JsonElement> entry : res.getParameters().entrySet())
                        {
                            resultValue.put(entry.getKey(), entry.getValue().toString());
                        }
                    }

                    aiService.stopListening();

                    loadingIndicatorView.setVisibility(View.GONE);

                    chatEditText.setEnabled(true);

                    recordImageView.setVisibility(View.VISIBLE);

                    processAIResponse(resultValue);

                    switchSearch(KWS_SEARCH);
                }

            });
        }
    }

    private void processAIResponse(Map<String,String> response)
    {
        if (NetworkStatus.isConnected(view.getContext()) && NetworkStatus.isConnectedFast(view.getContext()))
        {
            final String resultSpeech = response.get("speech");
            final String resultIntent = response.get("intentName");

            if (response.containsKey("any"))
            {
                resultAny = response.get("any").toLowerCase();
            }
            else
            {
                resultAny = "";
            }

            if (response.containsKey("device-action"))
            {
                resultDeviceAction = response.get("device-action").toLowerCase();
            }
            else
            {
                resultDeviceAction = "";
            }

            if (response.containsKey("phone-number"))
            {
                resultNumber = response.get("phone-number");
            }
            else
            {
                resultNumber = "";
            }

            if (response.containsKey("email"))
            {
                resultMail = response.get("email");
            }
            else
            {
                resultMail = "";
            }

            if (response.containsKey("message-action"))
            {
                resultMessageAction = response.get("message-action");
            }
            else
            {
                resultMessageAction = "";
            }

            if (response.containsKey("name"))
            {
                resultContactName = response.get("name");
            }
            else
            {
                resultContactName = "";
            }

            if (resultSpeech != null)
            {
                speak(resultSpeech);

                DatabaseReference messageRef = botChattingRef.child(botId).child(onlineUserId).push();

                String messageKey = messageRef.getKey();

                DateFormat df = new SimpleDateFormat("h:mm a", Locale.US);
                final String time = df.format(Calendar.getInstance().getTime());

                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);
                Date date = new Date();

                String todayDate = formatter.format(date);

                Map<String, Object> chatMessage = new HashMap<>();

                chatMessage.put("message", resultSpeech);
                chatMessage.put("from", botId);
                chatMessage.put("type", "text");
                chatMessage.put("key", messageKey);
                chatMessage.put("time", time);
                chatMessage.put("date", todayDate);

                if (messageKey != null)
                {
                    botChattingRef.child(botId).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                switch (resultIntent) {
                                    case "device.settings.anything.check":

                                        if (resultAny.contains("hotspot")) {
                                            boolean wifiStatus = HotspotApp.isApOn(view.getContext());

                                            if (wifiStatus) {
                                                String responseMessage = "Hotspot is enabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            } else {
                                                String responseMessage = "Hotspot is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        }
                                        else if (resultAny.contains("wifi") || resultAny.contains("wi-fi"))
                                        {
                                            if (checkPermission(PermissionUtil.READ_ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED)
                                            {
                                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_WIFI_STATE))
                                                {
                                                    showPermissionExplanation(PermissionUtil.READ_ACCESS_WIFI_STATE);
                                                }
                                                else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_ACCESS_WIFI_STATE))
                                                {
                                                    requestPermission(PermissionUtil.READ_ACCESS_WIFI_STATE);
                                                    permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_ACCESS_WIFI_STATE);
                                                }
                                                else
                                                {
                                                    Toast.makeText(view.getContext(), "Please allow wifi permission in your app settings", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent();
                                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                    intent.setData(uri);
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                            else
                                            {
                                                WifiManager wifi = (WifiManager) view.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                                                if (wifi != null && wifi.isWifiEnabled()) {
                                                    String responseMessage = "Wifi is enabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                } else {
                                                    String responseMessage = "Wifi is disabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                }
                                            }
                                        } else if (resultAny.contains("bluetooth"))
                                        {
                                            if (checkPermission(PermissionUtil.READ_BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
                                            {
                                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.BLUETOOTH))
                                                {
                                                    showPermissionExplanation(PermissionUtil.READ_BLUETOOTH);
                                                }
                                                else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_BLUETOOTH))
                                                {
                                                    requestPermission(PermissionUtil.READ_BLUETOOTH);
                                                    permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_BLUETOOTH);
                                                }
                                                else
                                                {
                                                    Toast.makeText(view.getContext(), "Please allow bluetooth permission in your app settings", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent();
                                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                    intent.setData(uri);
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                            else
                                            {
                                                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                                                if (mBluetoothAdapter == null) {
                                                    String responseMessage = "Your device does not support Bluetooth";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                } else {
                                                    if (mBluetoothAdapter.isEnabled()) {
                                                        String responseMessage = "Bluetooth is enable";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    } else {
                                                        String responseMessage = "Bluetooth is disabled";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    }
                                                }
                                            }
                                        } else if (resultAny.contains("gps"))
                                        {
                                            if (checkPermission(PermissionUtil.READ_ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                                            {
                                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.ACCESS_FINE_LOCATION))
                                                {
                                                    showPermissionExplanation(PermissionUtil.READ_ACCESS_FINE_LOCATION);
                                                }
                                                else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_ACCESS_FINE_LOCATION))
                                                {
                                                    requestPermission(PermissionUtil.READ_ACCESS_FINE_LOCATION);
                                                    permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_ACCESS_FINE_LOCATION);
                                                }
                                                else
                                                {
                                                    Toast.makeText(view.getContext(), "Please allow location permission in your app settings", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent();
                                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                    intent.setData(uri);
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                            else
                                            {
                                                LocationManager manager = (LocationManager) view.getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                                                if (manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                                    String responseMessage = "GPS is enable";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                } else {
                                                    String responseMessage = "GPS is disabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                }
                                            }
                                        } else if (resultAny.contains("flight mode") || resultAny.contains("airplane mode")) {
                                            boolean flightMode = Settings.Global.getInt(view.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

                                            if (flightMode) {
                                                String responseMessage = "Flight mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            } else {
                                                String responseMessage = "Flight mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        } else if (resultAny.contains("silent mode")) {
                                            AudioManager audioManager = (AudioManager) view.getContext().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 0) {
                                                String responseMessage = "Silent mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            } else {
                                                String responseMessage = "Silent mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        } else if (resultAny.contains("vibrate mode") || resultAny.contains("vibration mode")) {
                                            AudioManager audioManager = (AudioManager) view.getContext().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 1) {
                                                String responseMessage = resultAny + " is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            } else {
                                                String responseMessage = resultAny + " is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        } else if (resultAny.contains("normal mode")) {
                                            AudioManager audioManager = (AudioManager) view.getContext().getSystemService(AUDIO_SERVICE);

                                            if (audioManager != null && audioManager.getRingerMode() == 2) {
                                                String responseMessage = "Normal mode is enable";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            } else {
                                                String responseMessage = "Normal mode is disabled";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        } else {
                                            Toast.makeText(view.getContext(), resultAny, Toast.LENGTH_SHORT).show();
                                        }
                                        break;

                                    case "device.settings.anything.on-off":

                                        if (resultAny.contains("hotspot")) {
                                            if (resultDeviceAction.equals("on")) {
                                                boolean wifiStatus = HotspotApp.isApOn(view.getContext());

                                                if (!wifiStatus) {
                                                    boolean isEnabled = HotspotApp.configApState(view.getContext(), "enable");

                                                    if (isEnabled) {
                                                        String responseMessage = "Hotspot is enabled";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    } else {
                                                        String responseMessage = "Error occurred while accessing Hotspot";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    }
                                                } else {
                                                    String responseMessage = "Hotspot is already in enabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                }
                                            } else {
                                                boolean wifiStatus = HotspotApp.isApOn(view.getContext());

                                                if (wifiStatus) {
                                                    boolean isDisabled = HotspotApp.configApState(view.getContext(), "disable");

                                                    if (isDisabled) {
                                                        String responseMessage = "Hotspot is disabled";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    } else {
                                                        String responseMessage = "Error occurred while accessing Hotspot";

                                                        speak(responseMessage);

                                                        storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                    }
                                                } else {
                                                    String responseMessage = "Hotspot is already in disabled";

                                                    speak(responseMessage);

                                                    storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                                }
                                            }
                                        }
                                        break;

                                    case "device.apps.open":

                                        if (resultAny.contains("browser") || resultAny.contains("google"))
                                        {
                                            if (checkPermission(PermissionUtil.READ_INTERNET) != PackageManager.PERMISSION_GRANTED)
                                            {
                                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.INTERNET))
                                                {
                                                    showPermissionExplanation(PermissionUtil.READ_INTERNET);
                                                }
                                                else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_INTERNET))
                                                {
                                                    requestPermission(PermissionUtil.READ_INTERNET);
                                                    permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_INTERNET);
                                                }
                                                else
                                                {
                                                    Toast.makeText(view.getContext(), "Please allow internet permission in your app settings", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent();
                                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                    intent.setData(uri);
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                            else
                                            {
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                        } else if (resultAny.contains("message")) {
                                            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                                            smsIntent.setType("vnd.android-dir/mms-sms");
                                            smsIntent.putExtra("address", "");
                                            smsIntent.putExtra("sms_body", "");
                                            view.getContext().startActivity(smsIntent);
                                        } else if (resultAny.contains("email") || resultAny.contains("mail") || resultAny.contains("e-mail") || resultAny.contains("gmail") || resultAny.contains("g-mail")) {
                                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                                            intent.setData(Uri.parse("mailto:"));

                                            if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(intent);
                                            }
                                        } else if (resultAny.contains("settings")) {
                                            if (resultAny.contains("wifi") || resultAny.contains("wi-fi")) {

                                                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("wireless")) {
                                                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("flight mode") || resultAny.contains("airplane mode")) {
                                                Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("apn")) {
                                                Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("bluetooth")) {
                                                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("date")) {
                                                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("locale")) {
                                                Intent intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("input method")) {
                                                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("display")) {
                                                Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("security")) {
                                                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("location source")) {
                                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("internal storage")) {
                                                Intent intent = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else if (resultAny.contains("memory card")) {
                                                Intent intent = new Intent(Settings.ACTION_MEMORY_CARD_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            } else {
                                                Intent intent = new Intent(Settings.ACTION_SETTINGS);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                        } else if (resultAny.contains("image gallery") || resultAny.contains("gallery") || resultAny.contains("video gallery")) {
                                            Intent intent = new Intent();
                                            intent.setAction(android.content.Intent.ACTION_VIEW);
                                            intent.setType("image/*");

                                            if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(intent);
                                            }
                                        } else if (resultAny.contains("camera") || resultAny.contains("video camera")) {
                                            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

                                            if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(intent);
                                            }
                                        }
                                        break;
                                    case "search.web.anything":

                                        if (checkPermission(PermissionUtil.READ_INTERNET) != PackageManager.PERMISSION_GRANTED)
                                        {
                                            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.INTERNET))
                                            {
                                                showPermissionExplanation(PermissionUtil.READ_INTERNET);
                                            }
                                            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_INTERNET))
                                            {
                                                requestPermission(PermissionUtil.READ_INTERNET);
                                                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_INTERNET);
                                            }
                                            else
                                            {
                                                Toast.makeText(view.getContext(), "Please allow internet permission in your app settings", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent();
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                intent.setData(uri);
                                                view.getContext().startActivity(intent);
                                            }
                                        }
                                        else
                                        {
                                            if (resultDeviceAction.contains("search")) {
                                                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                                intent.putExtra(SearchManager.QUERY, resultAny);

                                                if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                        }

                                        break;
                                    case "device.message.send":

                                        if (resultMessageAction.contains("mail") || isValidMail(resultMail)) {
                                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + resultMail));
                                            emailIntent.putExtra(Intent.EXTRA_TEXT, resultAny);

                                            if (emailIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(emailIntent);
                                            }
                                        } else if (resultMessageAction.contains("message") || isValidMobile(resultNumber)) {
                                            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                                            smsIntent.setType("vnd.android-dir/mms-sms");
                                            smsIntent.putExtra("address", resultNumber);
                                            smsIntent.putExtra("sms_body", resultAny);

                                            if (smsIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(smsIntent);
                                            }
                                        } else {
                                            String number = getContactNumber(resultContactName);

                                            if (number != null)
                                            {
                                                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                                                smsIntent.setType("vnd.android-dir/mms-sms");
                                                smsIntent.putExtra("address", number);
                                                smsIntent.putExtra("sms_body", resultAny);

                                                if (smsIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    view.getContext().startActivity(smsIntent);
                                                }
                                            } else {
                                                String responseMessage = resultContactName + " not found in your contact, please check or try again";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        }
                                        break;
                                    case "device.contact.call":

                                        if (isValidMobile(resultNumber))
                                        {
                                            if (checkPermission(PermissionUtil.READ_CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                                            {
                                                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.CALL_PHONE))
                                                {
                                                    showPermissionExplanation(PermissionUtil.READ_CALL_PHONE);
                                                }
                                                else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_CALL_PHONE))
                                                {
                                                    requestPermission(PermissionUtil.READ_CALL_PHONE);
                                                    permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_CALL_PHONE);
                                                }
                                                else
                                                {
                                                    Toast.makeText(view.getContext(), "Please allow phone call permission in your app settings", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent();
                                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                    Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                    intent.setData(uri);
                                                    view.getContext().startActivity(intent);
                                                }
                                            }
                                            else
                                            {
                                                Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + resultNumber));

                                                if (callIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                    if (ActivityCompat.checkSelfPermission(view.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                        return;
                                                    }

                                                    view.getContext().startActivity(callIntent);
                                                }
                                            }
                                        } else {

                                            String number = getContactNumber(resultAny);

                                            if (number != null)
                                            {
                                                if (checkPermission(PermissionUtil.READ_CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                                                {
                                                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.CALL_PHONE))
                                                    {
                                                        showPermissionExplanation(PermissionUtil.READ_CALL_PHONE);
                                                    }
                                                    else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_CALL_PHONE))
                                                    {
                                                        requestPermission(PermissionUtil.READ_CALL_PHONE);
                                                        permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_CALL_PHONE);
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(view.getContext(), "Please allow phone call permission in your app settings", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent();
                                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                        Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                        intent.setData(uri);
                                                        view.getContext().startActivity(intent);
                                                    }
                                                }
                                                else
                                                {
                                                    Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));

                                                    if (callIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                        if (ActivityCompat.checkSelfPermission(view.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                            return;
                                                        }

                                                        view.getContext().startActivity(callIntent);
                                                    }
                                                }
                                            } else {
                                                String responseMessage = resultContactName + " not found in your contact, please check or try again";

                                                speak(responseMessage);

                                                storeMessageDetails(view.getContext(), onlineUserId, botId, responseMessage);
                                            }
                                        }

                                        break;
                                    case "device.contact.create":

                                        if (checkPermission(PermissionUtil.READ_WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED)
                                        {
                                            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.WRITE_CONTACTS))
                                            {
                                                showPermissionExplanation(PermissionUtil.READ_WRITE_CONTACTS);
                                            }
                                            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_WRITE_CONTACTS))
                                            {
                                                requestPermission(PermissionUtil.READ_WRITE_CONTACTS);
                                                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_WRITE_CONTACTS);
                                            }
                                            else
                                            {
                                                Toast.makeText(view.getContext(), "Please allow write contacts permission in your app settings", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent();
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                                                intent.setData(uri);
                                                view.getContext().startActivity(intent);
                                            }
                                        }
                                        else
                                        {
                                            Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION, Uri.parse("tel:" + resultNumber));
                                            contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

                                            if (contactIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                                                view.getContext().startActivity(contactIntent);
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(view.getContext(), "Error while storing documents", Toast.LENGTH_SHORT).show();
                            Log.e("AssistantFragment", e.toString());
                            Crashlytics.log(Log.ERROR, "AssistantFragment", e.getMessage());
                        }
                    });
                }
            }
        }
        else
        {
            Toast.makeText(activity, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }

        switchSearch(KWS_SEARCH);
    }

    @Override
    public void onError(AIError error)
    {
        String errorMessage = error.getMessage();

        Crashlytics.log(errorMessage);

        aiService.stopListening();

        chatEditText.setEnabled(true);
        chatEditText.setText("");

        loadingIndicatorView.setVisibility(View.GONE);

        recordImageView.setVisibility(View.VISIBLE);

        switchSearch(KWS_SEARCH);
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    // POCKET RECOGNITION RESULT


    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();

        if (text.equals(KEYPHRASE))
        {
            if (NetworkStatus.isConnected(view.getContext()) && NetworkStatus.isConnectedFast(view.getContext()))
            {
                startToRecognizeText();
            }
            else
            {
                Toast.makeText(activity, "No Internet Connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {

    }

    @Override
    public void onError(Exception e)
    {
        Crashlytics.log(e.getMessage());
    }

    @Override
    public void onTimeout()
    {
        switchSearch(KWS_SEARCH);
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception>
    {
        private WeakReference<AssistantFragment> fragmentWeakReference;

        SetupTask(AssistantFragment fragment)
        {
            this.fragmentWeakReference = new WeakReference<>(fragment);
        }
        @Override
        protected Exception doInBackground(Void... params)
        {
            try
            {
                Assets assets = new Assets(fragmentWeakReference.get().view.getContext());
                File assetDir = assets.syncAssets();
                fragmentWeakReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result)
        {
            if (result != null)
            {
                Crashlytics.log(result.getMessage());
            }
            else
            {
                fragmentWeakReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull  int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                new SetupTask(this).execute();
            }
            else
            {
                activity.finish();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (recognizer != null)
        {
            recognizer.cancel();
            recognizer.shutdown();
        }

        if (aiService != null)
        {
            aiService.cancel();
            aiService.stopListening();
        }

        if (toSpeech != null)
        {
            toSpeech.stop();
            toSpeech.shutdown();
        }
    }

    @Override
    public void onStop()
    {
        if (toSpeech != null)
        {
            toSpeech.stop();
        }

        if (recognizer != null)
        {
            recognizer.stop();
        }
        super.onStop();
    }

    private void switchSearch(String searchName)
    {
        recognizer.stop();

        if (searchName.equals(KWS_SEARCH))
        {
            recognizer.startListening(searchName);
        }
    }

    private void startToRecognizeText()
    {
        recognizer.stop();

        aiService.startListening();

        chatEditText.setText("");
        chatEditText.setEnabled(false);

        if (recordImageView.getVisibility() == View.GONE)
        {
            loadingIndicatorView.setVisibility(View.VISIBLE);
        }
        else
        {
            recordImageView.setVisibility(View.GONE);

            loadingIndicatorView.setVisibility(View.VISIBLE);
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir)
                .getRecognizer();
        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    private boolean isValidMobile(String phone)
    {
        boolean check;

        check = !Pattern.matches("[a-zA-Z]+", phone) && phone.length() >= 6 && phone.length() <= 13;

        return check;
    }

    private boolean isValidMail(String email)
    {
        boolean check;
        Pattern p;
        Matcher m;

        String EMAIL_STRING = "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        p = Pattern.compile(EMAIL_STRING);

        m = p.matcher(email);
        check = m.matches();

        return check;
    }

    private String getContactNumber(String inputName)
    {
        if (checkPermission(PermissionUtil.READ_READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.READ_CONTACTS))
            {
                showPermissionExplanation(PermissionUtil.READ_READ_CONTACTS);
            }
            else if (permissionUtil.checkPermissionPreference(PermissionUtil.PERMISSION_READ_CONTACTS))
            {
                requestPermission(PermissionUtil.READ_READ_CONTACTS);
                permissionUtil.updatePermissionPreference(PermissionUtil.PERMISSION_READ_CONTACTS);
            }
            else
            {
                Toast.makeText(view.getContext(), "Please allow read contacts permission in your app settings", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",view.getContext().getPackageName(),null);
                intent.setData(uri);
                this.startActivity(intent);
            }

            return null;
        }
        else
        {
            String mobileNumber = null;

            Cursor cursor = view.getContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);

            if (cursor != null)
            {
                while (cursor.moveToNext())
                {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                    if (name.equals(inputName))
                    {
                        mobileNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                }
            }

            if (cursor != null)
            {
                cursor.close();
            }

            return mobileNumber;
        }
    }

    private void speak(String input)
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
            resultValue.put("phone-number",parameters.optString("phone-number"));
            resultValue.put("email",parameters.optString("email"));
            resultValue.put("message-action",parameters.optString("message-action"));
            resultValue.put("name",parameters.optString("name"));

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

    private static class AssistantResponse extends AsyncTask<String, Void, Map<String, String>>
    {
        private WeakReference<AssistantFragment> fragmentWeakReference;

        AssistantResponse(AssistantFragment assistantFragment)
        {
            this.fragmentWeakReference = new WeakReference<>(assistantFragment);
        }

        @Override
        protected Map<String, String> doInBackground(String... strings)
        {
            return getText(strings[0]);
        }

        @Override
        protected void onPostExecute(Map<String, String> response)
        {
            super.onPostExecute(response);

            fragmentWeakReference.get().processAIResponse(response);
        }
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

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.US);
        Date date = new Date();

        String todayDate = formatter.format(date);

        messageDate.put("type", "date");
        messageDate.put("today_date", getTodayDate());
        messageDate.put("from",online_key);
        messageDate.put("key",date_push_id);
        messageDate.put("date", todayDate);

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

    private void storeMessageDetails(final Context context, String onlineUserId, String botId, String message)
    {
        DatabaseReference messageRef = botChattingRef.child(botId).child(onlineUserId).push();

        String messageKey = messageRef.getKey();

        DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
        final String time = df.format(Calendar.getInstance().getTime());

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.US);
        Date date = new Date();

        String todayDate = formatter.format(date);

        Map<String,Object> chatMessage = new HashMap<>();

        chatMessage.put("message",message);
        chatMessage.put("from",onlineUserId);
        chatMessage.put("type","text");
        chatMessage.put("key",messageKey);
        chatMessage.put("time",time);
        chatMessage.put("date",todayDate);

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

    private int checkPermission(int permission)
    {
        int status = PackageManager.PERMISSION_DENIED;

        switch (permission)
        {
            case PermissionUtil.READ_ACCESS_NETWORK_STATE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.ACCESS_NETWORK_STATE);
                break;
            case PermissionUtil.READ_INTERNET:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.INTERNET);
                break;
            case PermissionUtil.READ_RECORD_AUDIO:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.RECORD_AUDIO);
                break;
            case PermissionUtil.READ_CHANGE_NETWORK_STATE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.CHANGE_NETWORK_STATE);
                break;
            case PermissionUtil.READ_CHANGE_WIFI_STATE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.CHANGE_WIFI_STATE);
                break;
            case PermissionUtil.READ_ACCESS_WIFI_STATE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.ACCESS_WIFI_STATE);
                break;
            case PermissionUtil.READ_BLUETOOTH:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.BLUETOOTH);
                break;
            case PermissionUtil.READ_BLUETOOTH_ADMIN:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.BLUETOOTH_ADMIN);
                break;
            case PermissionUtil.READ_ACCESS_FINE_LOCATION:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.ACCESS_FINE_LOCATION);
                break;
            case PermissionUtil.READ_READ_CONTACTS:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.READ_CONTACTS);
                break;
            case PermissionUtil.READ_CALL_PHONE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.CALL_PHONE);
                break;
            case PermissionUtil.READ_READ_EXTERNAL_STORAGE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.READ_EXTERNAL_STORAGE);
                break;
            case PermissionUtil.READ_WRITE_EXTERNAL_STORAGE:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE);
                break;
            case PermissionUtil.READ_CAMERA:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.CAMERA);
                break;
            case PermissionUtil.READ_SET_ALARM:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.SET_ALARM);
                break;
            case PermissionUtil.READ_WRITE_CONTACTS:
                status = ContextCompat.checkSelfPermission(view.getContext(),Manifest.permission.WRITE_CONTACTS);
                break;
        }
        return status;
    }

    private void requestPermission(int permission)
    {
        switch (permission)
        {
            case PermissionUtil.READ_ACCESS_NETWORK_STATE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_CONTACTS},PermissionUtil.REQUEST_ACCESS_NETWORK_STATE);
                break;
            case PermissionUtil.READ_INTERNET:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.INTERNET},PermissionUtil.REQUEST_INTERNET);
                break;
            case PermissionUtil.READ_RECORD_AUDIO:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.RECORD_AUDIO},PermissionUtil.REQUEST_RECORD_AUDIO);
                break;
            case PermissionUtil.READ_CHANGE_NETWORK_STATE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CHANGE_NETWORK_STATE},PermissionUtil.REQUEST_CHANGE_NETWORK_STATE);
                break;
            case PermissionUtil.READ_CHANGE_WIFI_STATE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CHANGE_WIFI_STATE},PermissionUtil.REQUEST_CHANGE_WIFI_STATE);
                break;
            case PermissionUtil.READ_ACCESS_WIFI_STATE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.ACCESS_WIFI_STATE},PermissionUtil.REQUEST_ACCESS_WIFI_STATE);
                break;
            case PermissionUtil.READ_BLUETOOTH:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.BLUETOOTH},PermissionUtil.REQUEST_BLUETOOTH);
                break;
            case PermissionUtil.READ_BLUETOOTH_ADMIN:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.BLUETOOTH_ADMIN},PermissionUtil.REQUEST_BLUETOOTH_ADMIN);
                break;
            case PermissionUtil.READ_ACCESS_FINE_LOCATION:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PermissionUtil.REQUEST_ACCESS_FINE_LOCATION);
                break;
            case PermissionUtil.READ_READ_CONTACTS:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_CONTACTS},PermissionUtil.REQUEST_READ_CONTACTS);
                break;
            case PermissionUtil.READ_CALL_PHONE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CALL_PHONE},PermissionUtil.REQUEST_CALL_PHONE);
                break;
            case PermissionUtil.READ_READ_EXTERNAL_STORAGE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},PermissionUtil.REQUEST_READ_EXTERNAL_STORAGE);
                break;
            case PermissionUtil.READ_WRITE_EXTERNAL_STORAGE:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PermissionUtil.REQUEST_WRITE_EXTERNAL_STORAGE);
                break;
            case PermissionUtil.READ_CAMERA:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CAMERA},PermissionUtil.REQUEST_CAMERA);
                break;
            case PermissionUtil.READ_SET_ALARM:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.SET_ALARM},PermissionUtil.REQUEST_SET_ALARM);
                break;
            case PermissionUtil.READ_WRITE_CONTACTS:
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_CONTACTS},PermissionUtil.REQUEST_WRITE_CONTACTS);
                break;
        }
    }

    private void showPermissionExplanation(final int permission)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

        switch (permission)
        {
            case PermissionUtil.READ_ACCESS_NETWORK_STATE:
                builder.setMessage("This app need to access your network state..");
                builder.setTitle("Network Permission Needed..");
                break;
            case PermissionUtil.READ_INTERNET:
                builder.setMessage("This app need to access your internet..");
                builder.setTitle("Internet Permission Needed..");
                break;
            case PermissionUtil.READ_RECORD_AUDIO:
                builder.setMessage("This app need to access your audio..");
                builder.setTitle("Audio Permission Needed..");
                break;
            case PermissionUtil.READ_CHANGE_NETWORK_STATE:
                builder.setMessage("This app need to access your network state..");
                builder.setTitle("Change Network State Permission Needed..");
                break;
            case PermissionUtil.READ_CHANGE_WIFI_STATE:
                builder.setMessage("This app need to access your wifi state..");
                builder.setTitle("Change Wifi State Permission Needed..");
                break;
            case PermissionUtil.READ_ACCESS_WIFI_STATE:
                builder.setMessage("This app need to access your wifi..");
                builder.setTitle("Wifi Permission Needed..");
                break;
            case PermissionUtil.READ_BLUETOOTH:
                builder.setMessage("This app need to access your bluetooth..");
                builder.setTitle("Bluetooth Permission Needed..");
                break;
            case PermissionUtil.READ_BLUETOOTH_ADMIN:
                builder.setMessage("This app need to access your bluetooth admin..");
                builder.setTitle("Bluetooth Admin Permission Needed..");
                break;
            case PermissionUtil.READ_ACCESS_FINE_LOCATION:
                builder.setMessage("This app need to access your location..");
                builder.setTitle("Location Permission Needed..");
                break;
            case PermissionUtil.READ_READ_CONTACTS:
                builder.setMessage("This app need to access your contacts..");
                builder.setTitle("Contacts Permission Needed..");
                break;
            case PermissionUtil.READ_CALL_PHONE:
                builder.setMessage("This app need to access your phone..");
                builder.setTitle("Phone Call Permission Needed..");
                break;
            case PermissionUtil.READ_READ_EXTERNAL_STORAGE:
                builder.setMessage("This app need to read files from your storage..");
                builder.setTitle("Read Storage Permission Needed..");
                break;
            case PermissionUtil.READ_WRITE_EXTERNAL_STORAGE:
                builder.setMessage("This app need to write files in your storage..");
                builder.setTitle("Write Storage Permission Needed..");
                break;
            case PermissionUtil.READ_CAMERA:
                builder.setMessage("This app need to access your camera..");
                builder.setTitle("Camera Permission Needed..");
                break;
            case PermissionUtil.READ_SET_ALARM:
                builder.setMessage("This app need to access your alarm..");
                builder.setTitle("Alarm Permission Needed..");
                break;
            case PermissionUtil.READ_WRITE_CONTACTS:
                builder.setMessage("This app need to write contacts in your contacts..");
                builder.setTitle("Write Contacts Permission Needed..");
                break;
        }

        builder.setPositiveButton("Allow", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (permission)
                {
                    case PermissionUtil.READ_ACCESS_NETWORK_STATE:
                        requestPermission(PermissionUtil.READ_ACCESS_NETWORK_STATE);
                        break;
                    case PermissionUtil.READ_INTERNET:
                        requestPermission(PermissionUtil.READ_INTERNET);
                        break;
                    case PermissionUtil.READ_RECORD_AUDIO:
                        requestPermission(PermissionUtil.READ_RECORD_AUDIO);
                        break;
                    case PermissionUtil.READ_CHANGE_NETWORK_STATE:
                        requestPermission(PermissionUtil.READ_CHANGE_NETWORK_STATE);
                        break;
                    case PermissionUtil.READ_CHANGE_WIFI_STATE:
                        requestPermission(PermissionUtil.READ_CHANGE_WIFI_STATE);
                        break;
                    case PermissionUtil.READ_ACCESS_WIFI_STATE:
                        requestPermission(PermissionUtil.READ_ACCESS_WIFI_STATE);
                        break;
                    case PermissionUtil.READ_BLUETOOTH:
                        requestPermission(PermissionUtil.READ_BLUETOOTH);
                        break;
                    case PermissionUtil.READ_BLUETOOTH_ADMIN:
                        requestPermission(PermissionUtil.READ_BLUETOOTH_ADMIN);
                        break;
                    case PermissionUtil.READ_ACCESS_FINE_LOCATION:
                        requestPermission(PermissionUtil.READ_ACCESS_FINE_LOCATION);
                        break;
                    case PermissionUtil.READ_READ_CONTACTS:
                        requestPermission(PermissionUtil.READ_READ_CONTACTS);
                        break;
                    case PermissionUtil.READ_CALL_PHONE:
                        requestPermission(PermissionUtil.READ_CALL_PHONE);
                        break;
                    case PermissionUtil.READ_READ_EXTERNAL_STORAGE:
                        requestPermission(PermissionUtil.READ_READ_EXTERNAL_STORAGE);
                        break;
                    case PermissionUtil.READ_WRITE_EXTERNAL_STORAGE:
                        requestPermission(PermissionUtil.READ_WRITE_EXTERNAL_STORAGE);
                        break;
                    case PermissionUtil.READ_CAMERA:
                        requestPermission(PermissionUtil.READ_CAMERA);
                        break;
                    case PermissionUtil.READ_SET_ALARM:
                        requestPermission(PermissionUtil.READ_SET_ALARM);
                        break;
                    case PermissionUtil.READ_WRITE_CONTACTS:
                        requestPermission(PermissionUtil.READ_WRITE_CONTACTS);
                        break;
                }
            }
        });

        builder.setNegativeButton("Deny", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
