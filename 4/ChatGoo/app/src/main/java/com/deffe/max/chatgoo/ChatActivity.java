package com.deffe.max.chatgoo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.Adapters.FriendsChattingRecyclerAdapter;
import com.deffe.max.chatgoo.Models.MessageTypesModel;
import com.deffe.max.chatgoo.Utils.NetworkStatus;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.deffe.max.chatgoo.Adapters.FriendsChattingRecyclerAdapter.CHAT_VIDEO_DOWNLOAD_ID;

public class ChatActivity extends AppCompatActivity
{
    private static final String TAG = ChatActivity.class.getSimpleName();

    private static final int VIDEO_GALLERY_REQUEST_CODE = 100;
    private static final int VIDEO_CAMERA_REQUEST_CODE = 101;

    private DatabaseReference chattingRef;

    private String onlineUserId,receiverUserId;

    private ArrayList<MessageTypesModel> friendsMessages = new ArrayList<>();

    private FriendsChattingRecyclerAdapter adapter;

    private AppCompatEditText friendsChatEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();

        if (intent.getExtras() != null)
        {
            receiverUserId = intent.getExtras().getString("receiver_id");
        }

        Toolbar chatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);

        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        chattingRef = FirebaseDatabase.getInstance().getReference();
        chattingRef.keepSynced(true);

        if (user != null)
        {
            onlineUserId = user.getUid();
        }

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final CircleImageView chatUserCircleImageView = findViewById(R.id.chat_user_circle_image_view);
        final AppCompatTextView chatUserNameTextView = findViewById(R.id.chat_user_name_text_view);
        friendsChatEditText = findViewById(R.id.chatting_friend_edit_text);
        AppCompatImageView friendChatSendImageView = findViewById(R.id.friend_chat_send_image_view);
        AppCompatImageView optionVideo = findViewById(R.id.option_video);
        AppCompatImageView optionImage = findViewById(R.id.option_camera);
        AppCompatImageView optionOCR = findViewById(R.id.option_text_recog);

        chattingRef.child("Users").child(receiverUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                Object objectName = dataSnapshot.child("user_name").getValue();
                Object objectThumb = dataSnapshot.child("user_profile_thumb_img").getValue();

                if (objectName != null)
                {
                    String userName = objectName.toString();

                    chatUserNameTextView.setText(userName);
                }

                if (objectThumb != null)
                {
                    final String userThumb = objectThumb.toString();

                    Picasso.with(ChatActivity.this).load(userThumb).networkPolicy(NetworkPolicy.OFFLINE).into(chatUserCircleImageView, new Callback()
                    {
                        @Override
                        public void onSuccess()
                        {

                        }

                        @Override
                        public void onError()
                        {
                            Picasso.with(ChatActivity.this).load(userThumb).placeholder(R.drawable.img_sel).into(chatUserCircleImageView);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

        final RecyclerView friendChattingRecycler = findViewById(R.id.friends_chatting_recycler_view);
        friendChattingRecycler.setLayoutManager(new LinearLayoutManager(ChatActivity.this,LinearLayoutManager.VERTICAL,false));
        adapter = new FriendsChattingRecyclerAdapter(ChatActivity.this,friendsMessages,onlineUserId);
        friendChattingRecycler.setAdapter(adapter);

        chattingRef.child("Friends_Messages").child(onlineUserId).child(receiverUserId).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                friendsMessages.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    MessageTypesModel model = snapshot.getValue(MessageTypesModel.class);
                    friendsMessages.add(model);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });

        friendChatSendImageView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (NetworkStatus.isConnected(ChatActivity.this) && NetworkStatus.isConnectedFast(ChatActivity.this))
                {
                    String message = friendsChatEditText.getText().toString();

                    if (TextUtils.isEmpty(message) && !message.equals(" "))
                    {
                        Toast.makeText(ChatActivity.this, "Please enter any text", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        friendsChatEditText.setText("");

                        if (checkDate(ChatActivity.this, onlineUserId, receiverUserId))
                        {
                            storeTextMessageDetails(ChatActivity.this, onlineUserId, receiverUserId,message);
                        }
                        else
                        {
                            storeTextMessageDetails(ChatActivity.this, onlineUserId, receiverUserId, message);
                        }
                    }
                }
                else
                {
                    Toast.makeText(ChatActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        optionVideo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                View view = getLayoutInflater().inflate(R.layout.video_bottom_sheet,null);

                BottomSheetDialog dialog = new BottomSheetDialog(ChatActivity.this);
                dialog.setContentView(view);
                dialog.show();

                LinearLayout videoGallery = dialog.findViewById(R.id.video_gallery);
                LinearLayout videoCamera = dialog.findViewById(R.id.video_camera);

                if (videoGallery != null)
                {
                    videoGallery.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Intent galleryVideoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                            galleryVideoIntent.setType("video/*");
                            startActivityForResult(galleryVideoIntent,VIDEO_GALLERY_REQUEST_CODE);
                        }
                    });
                }

                if (videoCamera != null)
                {
                    videoCamera.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (hasCamera())
                            {
                                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());

                                File videoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ChatGoo/Video/" + "VID_" + timestamp + "_.mp4");

                                Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                Uri videoUri = Uri.fromFile(videoFile);

                                videoIntent.putExtra(MediaStore.EXTRA_OUTPUT,videoUri);
                                startActivityForResult(videoIntent,100);
                            }
                        }
                    });
                }
            }
        });

        optionImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(ChatActivity.this);
            }
        });

        optionOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(ChatActivity.this,VideoCameraActivity.class),1005);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if (requestCode == VIDEO_CAMERA_REQUEST_CODE)
            {
                Uri uri = data.getData();

                if (uri != null)
                {
                    if (NetworkStatus.isConnected(ChatActivity.this) && NetworkStatus.isConnectedFast(ChatActivity.this))
                    {
                        if (checkDate(ChatActivity.this, onlineUserId, receiverUserId))
                        {
                            storeVideoMessageDetails(ChatActivity.this, onlineUserId, receiverUserId,uri);
                        }
                        else
                        {
                            storeVideoMessageDetails(ChatActivity.this, onlineUserId, receiverUserId, uri);
                        }
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if (requestCode == VIDEO_GALLERY_REQUEST_CODE)
            {
                Uri uri = data.getData();

                Uri fileUri = Uri.parse(getFilePathFromUri(ChatActivity.this,uri));

                if (NetworkStatus.isConnected(ChatActivity.this) && NetworkStatus.isConnectedFast(ChatActivity.this))
                {
                    if (checkDate(ChatActivity.this, onlineUserId, receiverUserId))
                    {
                        storeVideoMessageDetails(ChatActivity.this, onlineUserId, receiverUserId,fileUri);
                    }
                    else
                    {
                        storeVideoMessageDetails(ChatActivity.this, onlineUserId, receiverUserId, fileUri);
                    }
                }
                else
                {
                    Toast.makeText(ChatActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
            else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
            {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);

                Uri uri = result.getUri();

                Uri storageUri = null;
                boolean created = false;

                final File localFile = new File(Environment.getExternalStorageDirectory() + "/ChatGoo/Images");

                if (!localFile.exists())
                {
                    created = localFile.mkdirs();
                }

                if (created)
                {
                    try
                    {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

                        File mediaFile = new File(localFile.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

                        storageUri = copyFileToFolder(new File(getFilePathFromUri(ChatActivity.this,uri)), mediaFile);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    if (NetworkStatus.isConnected(ChatActivity.this) && NetworkStatus.isConnectedFast(ChatActivity.this))
                    {
                        if (checkDate(ChatActivity.this, onlineUserId, receiverUserId))
                        {
                            storeImageMessageDetails(ChatActivity.this, onlineUserId, receiverUserId,storageUri);
                        }
                        else
                        {
                            storeImageMessageDetails(ChatActivity.this, onlineUserId, receiverUserId, storageUri);
                        }
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if (requestCode == 1005)
            {
                String text = data.getStringExtra("text");

                friendsChatEditText.setText(text);
            }
        }
        else if (resultCode == RESULT_CANCELED)
        {
            Toast.makeText(this, "Video recording cancelled", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Failed to record video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (adapter.isDownloading)
        {
            adapter.notificationManagerCompat.cancel(CHAT_VIDEO_DOWNLOAD_ID);
            adapter.builder = null;

            adapter.isDownloading = false;
        }
    }

    private Uri copyFileToFolder(File sourceFile, File destinationFile) throws IOException
    {
        FileChannel source;
        FileChannel destination;

        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destinationFile).getChannel();

        if (source != null)
        {
            destination.transferFrom(source,0,source.size());
        }

        if (source != null)
        {
            source.close();
        }
        destination.close();

        return Uri.fromFile(destinationFile);
    }

    private String getFilePathFromUri(Context context,Uri contentUri)
    {
        String filePath = null;

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat)
        {
            filePath = generateFromKitKat(context, contentUri);
        }

        if (filePath != null)
        {
            return filePath;
        }

        Cursor cursor = context.getContentResolver().query(contentUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);

        if (cursor != null)
        {
            if (cursor.moveToFirst())
            {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                filePath = cursor.getString(column_index);
            }

            cursor.close();
        }
        return filePath == null ? contentUri.getPath() : filePath;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String generateFromKitKat(Context context, Uri contentUri)
    {
        String filePath = null;

        if (DocumentsContract.isDocumentUri(context,contentUri))
        {
            String wholeID = DocumentsContract.getDocumentId(contentUri);

            String id = wholeID.split(":")[1];

            String[] column = {MediaStore.Video.Media.DATA};
            String sel = MediaStore.Video.Media._ID + "=?";

            Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,column,sel,new String[]{id},null);

            int columnIndex = 0;

            if (cursor != null)
            {
                columnIndex = cursor.getColumnIndex(column[0]);
            }

            if (cursor != null && cursor.moveToFirst())
            {
                filePath = cursor.getString(columnIndex);
                cursor.close();
            }
        }

        return filePath;
    }

    public static String getStringSizeFromFile(long size)
    {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMb = sizeKb * sizeKb;
        float sizeGb = sizeMb * sizeMb;
        float sizeTera = sizeGb * sizeGb;

        if (size < sizeMb)
        {
            return decimalFormat.format(size / sizeKb) + " Kb";
        }
        else if (size < sizeGb)
        {
            return decimalFormat.format(size / sizeMb) + " Mb";
        }
        else if (size < sizeTera)
        {
            return decimalFormat.format(size / sizeGb) + " Gb";
        }
        return "";
    }

    private boolean hasCamera()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
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

    private void storeDateRef(Context context, String online_key, String friend_key)
    {
        SharedPreferences preferences = context.getSharedPreferences(online_key,MODE_PRIVATE);

        if (preferences != null)
        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(friend_key,getTodayDate());
            editor.apply();
        }
    }

    private String readDateRef(Context context, String online_key, String friend_key)
    {
        String todayDate,checkDate = null;

        SharedPreferences date = context.getSharedPreferences(online_key,MODE_PRIVATE);

        if (date != null)
        {
            checkDate = date.getString(friend_key,null);
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

    private boolean checkDate(Context context, String online, String friend)
    {
        String today = readDateRef(context,online,friend);

        boolean result = false;

        if (today == null)
        {
            updateDateRef(context,online,friend);

            result = true;
        }
        else
        {
            try
            {
                String checkDate = formatToYesterdayOrToday(today);

                if (!checkDate.equals("Today"))
                {
                    updateDateRef(context,online,friend);

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

    private void updateDateRef(final Context context, final String online_key, final String friend_key)
    {
        storeDateRef(context,online_key,friend_key);

        DatabaseReference dateRef = chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).push();

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
            chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).child(date_push_id).updateChildren(messageDate).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if (task.isSuccessful())
                    {
                        chattingRef.child("Friends_Messages").child(friend_key).child(onlineUserId).child(date_push_id).updateChildren(messageDate).addOnFailureListener(new OnFailureListener()
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
            }).addOnFailureListener(new OnFailureListener()
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

    private void storeTextMessageDetails(final Context context, final String onlineUserId, final String friend_key, String message)
    {
        DatabaseReference messageRef = chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).push();

        final String messageKey = messageRef.getKey();

        DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
        final String time = df.format(Calendar.getInstance().getTime());

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.US);
        Date date = new Date();

        String todayDate = formatter.format(date);

        final Map<String,Object> chatMessage = new HashMap<>();

        chatMessage.put("message",message);
        chatMessage.put("from",onlineUserId);
        chatMessage.put("type","text");
        chatMessage.put("key",messageKey);
        chatMessage.put("time",time);
        chatMessage.put("date",todayDate);

        if (messageKey != null)
        {
            chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).child(messageKey).updateChildren(chatMessage).addOnCompleteListener(new OnCompleteListener<Void>()
            {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if (task.isSuccessful())
                    {
                        chattingRef.child("Friends_Messages").child(friend_key).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnFailureListener(new OnFailureListener()
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
            }).addOnFailureListener(new OnFailureListener()
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

    private void storeImageMessageDetails(final Context context, final String onlineUserId, final String friend_key, Uri imageUri)
    {
        DatabaseReference messageRef = chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).push();

        final String messageKey = messageRef.getKey();

        final StorageReference videoRef = FirebaseStorage.getInstance().getReference().child("Chatting").child("Images").child(messageKey + ".jpg");

        DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
        final String time = df.format(Calendar.getInstance().getTime());

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.US);
        Date date = new Date();

        final String todayDate = formatter.format(date);

        if (messageKey != null)
        {
            UploadTask uploadTask = videoRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>()
            {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task)
                {
                    if (!task.isSuccessful())
                    {
                        Exception exception = task.getException();

                        if (exception != null)
                        {
                            Log.e(TAG, exception.getMessage());
                            Crashlytics.log(Log.ERROR, TAG, exception.getMessage());
                            Toast.makeText(ChatActivity.this, "Error while uploading video in storage " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    return videoRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>()
            {
                @Override
                public void onComplete(@NonNull Task<Uri> task)
                {
                    if (task.isSuccessful())
                    {
                        final String downloadUrl = task.getResult().toString();

                        final Map<String,Object> chatMessage = new HashMap<>();

                        chatMessage.put("from",onlineUserId);
                        chatMessage.put("type","image");
                        chatMessage.put("key",messageKey);
                        chatMessage.put("time",time);
                        chatMessage.put("date",todayDate);
                        chatMessage.put("message",downloadUrl);

                        chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).child(messageKey).updateChildren(chatMessage).addOnCompleteListener(new OnCompleteListener<Void>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Void> task)
                            {
                                if (task.isSuccessful())
                                {
                                    chattingRef.child("Friends_Messages").child(friend_key).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnFailureListener(new OnFailureListener()
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
                        }).addOnFailureListener(new OnFailureListener()
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
            });
        }
    }

    private void storeVideoMessageDetails(final Context context, final String onlineUserId, final String friend_key, Uri videoUri)
    {
        DatabaseReference messageRef = chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).push();

        final String messageKey = messageRef.getKey();

        final StorageReference videoRef = FirebaseStorage.getInstance().getReference().child("Chatting").child("Videos").child(messageKey + ".mp4");

        final StorageReference videoThumbRef = FirebaseStorage.getInstance().getReference().child("Chatting").child("Videos").child("Thumbs").child(messageKey + ".mp4");

        DateFormat df = new SimpleDateFormat("h:mm a",Locale.US);
        final String time = df.format(Calendar.getInstance().getTime());

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",Locale.US);
        Date date = new Date();

        final String todayDate = formatter.format(date);

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        Bitmap thumb;

        boolean created;

        mediaMetadataRetriever.setDataSource(videoUri.getPath());

        thumb = mediaMetadataRetriever.getFrameAtTime();

        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        created = tempDir.mkdir();
        File tempFile;

        if (created)
        {
            try
            {
                tempFile = File.createTempFile("ChatGoo",".jpg",tempDir);

                if (thumb != null)
                {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                    thumb.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

                    byte[] bitmapData = byteArrayOutputStream.toByteArray();

                    FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                    fileOutputStream.write(bitmapData);
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    final Uri thumbUri = Uri.fromFile(tempFile);

                    if (messageKey != null)
                    {
                        UploadTask uploadTask = videoRef.putFile(videoUri);

                        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>()
                        {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (!task.isSuccessful())
                                {
                                    Exception exception = task.getException();

                                    if (exception != null)
                                    {
                                        Log.e(TAG, exception.getMessage());
                                        Crashlytics.log(Log.ERROR, TAG, exception.getMessage());
                                        Toast.makeText(ChatActivity.this, "Error while uploading video in storage " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                return videoRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>()
                        {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task)
                            {
                                if (task.isSuccessful())
                                {
                                    final String videoDownloadUrl = task.getResult().toString();

                                    videoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>()
                                    {
                                        @Override
                                        public void onSuccess(StorageMetadata storageMetadata)
                                        {
                                            long fileSize = storageMetadata.getSizeBytes();

                                            final String size = getStringSizeFromFile(fileSize);

                                            UploadTask thumbUploadingTask = videoThumbRef.putFile(thumbUri);

                                            thumbUploadingTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>()
                                            {
                                                @Override
                                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                    if (!task.isSuccessful())
                                                    {
                                                        Exception exception = task.getException();

                                                        if (exception != null)
                                                        {
                                                            Log.e(TAG, exception.getMessage());
                                                            Crashlytics.log(Log.ERROR, TAG, exception.getMessage());
                                                            Toast.makeText(ChatActivity.this, "Error while uploading video in storage " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                    return videoThumbRef.getDownloadUrl();
                                                }
                                            }).addOnCompleteListener(new OnCompleteListener<Uri>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Uri> task)
                                                {
                                                    if (task.isSuccessful())
                                                    {
                                                        final String videoThumbDownloadUrl = task.getResult().toString();

                                                        final Map<String,Object> chatMessage = new HashMap<>();

                                                        chatMessage.put("from",onlineUserId);
                                                        chatMessage.put("type","video");
                                                        chatMessage.put("key",messageKey);
                                                        chatMessage.put("time",time);
                                                        chatMessage.put("date",todayDate);
                                                        chatMessage.put("message",videoDownloadUrl);
                                                        chatMessage.put("video_thumb_url",videoThumbDownloadUrl);
                                                        chatMessage.put("size",size);

                                                        chattingRef.child("Friends_Messages").child(onlineUserId).child(friend_key).child(messageKey).updateChildren(chatMessage).addOnCompleteListener(new OnCompleteListener<Void>()
                                                        {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    chattingRef.child("Friends_Messages").child(friend_key).child(onlineUserId).child(messageKey).updateChildren(chatMessage).addOnFailureListener(new OnFailureListener()
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
                                                        }).addOnFailureListener(new OnFailureListener()
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
                                            }).addOnFailureListener(new OnFailureListener()
                                            {
                                                @Override
                                                public void onFailure(@NonNull Exception e)
                                                {

                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        overridePendingTransition(R.anim.slide_in_down,R.anim.slide_out_down);
    }
}
