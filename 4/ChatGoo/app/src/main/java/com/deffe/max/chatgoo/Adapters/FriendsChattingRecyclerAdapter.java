package com.deffe.max.chatgoo.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.deffe.max.chatgoo.Models.MessageTypesModel;
import com.deffe.max.chatgoo.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.deffe.max.chatgoo.ChatActivity.getStringSizeFromFile;

public class FriendsChattingRecyclerAdapter extends RecyclerView.Adapter<FriendsChattingRecyclerAdapter.FriendsChattingViewHolder>
{
    private Context context;
    private ArrayList<MessageTypesModel> friendsMessages;
    private String onlineUserId;

    public boolean isDownloading = false;

    public NotificationCompat.Builder builder;
    public NotificationManagerCompat notificationManagerCompat;

    private static final String CHAT_VIDEO_DOWNLOAD_CHANNEL_ID = "com.deffe.max.chatgoo.Adapters.FriendsChattingRecyclerAdapter";
    public static final int CHAT_VIDEO_DOWNLOAD_ID = (int) ((new Date().getTime() / 100L) % Integer.MAX_VALUE);

    public FriendsChattingRecyclerAdapter(Context context, ArrayList<MessageTypesModel> friendsMessages,String onlineUserId)
    {
        this.context = context;
        this.friendsMessages = friendsMessages;
        this.onlineUserId = onlineUserId;
    }

    @NonNull
    @Override
    public FriendsChattingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.friends_chatting_layout_items,parent,false);

        return new FriendsChattingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FriendsChattingViewHolder holder, int position)
    {
        final MessageTypesModel model = friendsMessages.get(position);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        if (model.getFrom().equals(onlineUserId))
        {
            holder.thumb.setVisibility(GONE);

            switch (model.getType())
            {
                case "text":
                    holder.receiverTextCardView.setVisibility(GONE);
                    holder.receiverImageCardView.setVisibility(GONE);
                    holder.receiverVideoCardView.setVisibility(GONE);

                    holder.senderImageCardView.setVisibility(GONE);
                    holder.senderVideoCardView.setVisibility(GONE);

                    holder.senderTextCardView.setVisibility(VISIBLE);
                    holder.senderMessageText.setText(model.getMessage());
                    holder.senderMessageTextTime.setText(model.getTime());
                    break;
                case "image":
                    holder.receiverTextCardView.setVisibility(GONE);
                    holder.receiverImageCardView.setVisibility(GONE);
                    holder.receiverVideoCardView.setVisibility(GONE);

                    holder.senderTextCardView.setVisibility(GONE);
                    holder.senderVideoCardView.setVisibility(GONE);

                    holder.senderImageCardView.setVisibility(VISIBLE);
                    holder.senderMessageImageTime.setText(model.getTime());
                    holder.setSenderMessageImage(model.getMessage());

                    holder.senderMessageImage.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse("file://"+ model.getMessage()),"image/*");
                            context.startActivity(intent);
                        }
                    });

                    break;
                case "video":
                    holder.receiverTextCardView.setVisibility(GONE);
                    holder.receiverImageCardView.setVisibility(GONE);
                    holder.receiverVideoCardView.setVisibility(GONE);

                    holder.senderTextCardView.setVisibility(GONE);
                    holder.senderImageCardView.setVisibility(GONE);

                    holder.senderVideoCardView.setVisibility(VISIBLE);
                    holder.senderMessageVideoTime.setText(model.getTime());
                    holder.senderMessageVideoSize.setText(model.getSize());
                    holder.setSenderMessageVideoImage(model.getVideo_thumb_url());

                    final String videoResult = getChatLocalStorageRef(context,model.getKey());

                    if (videoResult != null)
                    {
                        holder.senderMessageVideoSize.setVisibility(GONE);
                        holder.senderMessageVideoPlayBtn.setVisibility(VISIBLE);
                    }
                    else
                    {
                        holder.senderMessageVideoPlayBtn.setVisibility(GONE);
                        holder.senderMessageVideoSize.setVisibility(VISIBLE);
                    }

                    holder.senderMessageVideoPlayBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            File file = null;

                            if (videoResult != null)
                            {
                                file = new File(videoResult);
                            }

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                            context.startActivity(intent);
                        }
                    });

                    holder.senderMessageVideoSize.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (!isDownloading)
                            {
                                Toast.makeText(context, "Video Downloading Started Please wait.... ", Toast.LENGTH_LONG).show();

                                holder.senderMessageVideoSize.setVisibility(GONE);
                                holder.senderMessageVideoDownloadingTextView.setVisibility(VISIBLE);

                                isDownloading = true;

                                notificationManagerCompat = NotificationManagerCompat.from(context);
                                builder = new NotificationCompat.Builder(context,CHAT_VIDEO_DOWNLOAD_CHANNEL_ID);
                                builder.setContentTitle("Chatting Video Download")
                                        .setSmallIcon(R.drawable.ic_file_download)
                                        .setPriority(NotificationCompat.PRIORITY_LOW)
                                        .setOngoing(true)
                                        .setAutoCancel(true);

                                StorageReference downloadRef = FirebaseStorage.getInstance().getReferenceFromUrl(model.getMessage());

                                final File localFile = new File(Environment.getExternalStorageDirectory() + "/ChatGoo/","Videos");

                                boolean created = false;

                                if (!localFile.exists())
                                {
                                    created = localFile.mkdirs();
                                }

                                if (created)
                                {
                                    final File downloadFile = new File(localFile,"VID_" + System.currentTimeMillis()  + ".mp4");

                                    downloadRef.getFile(downloadFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task)
                                        {
                                            builder.setContentText("Download Completed").setProgress(0, 0, false);
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());

                                            if (task.isSuccessful())
                                            {
                                                Toast.makeText(context, "Video Downloaded", Toast.LENGTH_SHORT).show();

                                                Uri file = Uri.fromFile(downloadFile);

                                                setChatLocalStorageRef(context,model.getKey(),file.toString());

                                                notificationManagerCompat.cancel(CHAT_VIDEO_DOWNLOAD_ID);
                                                builder = null;

                                                isDownloading = false;

                                                holder.senderMessageVideoDownloadingTextView.setVisibility(GONE);
                                                holder.senderMessageVideoPlayBtn.setVisibility(VISIBLE);

                                                holder.senderMessageVideoPlayBtn.setOnClickListener(new View.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(View v)
                                                    {
                                                        if (videoResult != null)
                                                        {
                                                            File file = new File(videoResult);

                                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                                                            context.startActivity(intent);
                                                        }
                                                        else
                                                        {
                                                            final String result = getChatLocalStorageRef(context,model.getKey());

                                                            File file = null;

                                                            if (result != null)
                                                            {
                                                                file = new File(result);
                                                            }

                                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                                                            context.startActivity(intent);
                                                        }
                                                    }
                                                });
                                            }

                                            if (task.isCanceled())
                                            {
                                                isDownloading = false;

                                                Toast.makeText(context, "Downloading Cancelled", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener()
                                    {
                                        @Override
                                        public void onFailure(@NonNull Exception e)
                                        {
                                            builder.setContentTitle("Downloading failed").setOngoing(false);
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());

                                            isDownloading = false;

                                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>()
                                    {
                                        @Override
                                        public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot)
                                        {
                                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                            builder.setProgress(100, (int) progress, false).setContentInfo((int) progress + "%").setContentText(getStringSizeFromFile(taskSnapshot.getBytesTransferred()) + " / " + getStringSizeFromFile(taskSnapshot.getTotalByteCount()));
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());
                                        }
                                    });
                                }

                            }
                            else
                            {
                                Toast.makeText(context, "Please wait another downloading is process..!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    break;
            }
        }
        else
        {
            holder.thumb.setVisibility(VISIBLE);

            userRef.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    Object objectThumb = dataSnapshot.child("user_profile_thumb_img").getValue();

                    if (objectThumb != null)
                    {
                        String userThumb = objectThumb.toString();

                        holder.setReceiverProfileImage(userThumb);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    Crashlytics.log(databaseError.getMessage());
                }
            });

            switch (model.getType())
            {
                case "text":
                    holder.senderTextCardView.setVisibility(GONE);
                    holder.senderImageCardView.setVisibility(GONE);
                    holder.senderVideoCardView.setVisibility(GONE);

                    holder.receiverImageCardView.setVisibility(GONE);
                    holder.receiverVideoCardView.setVisibility(GONE);

                    holder.receiverTextCardView.setVisibility(VISIBLE);
                    holder.receiverMessageText.setText(model.getMessage());
                    holder.receiverMessageTextTime.setText(model.getTime());
                    break;
                case "image":
                    holder.senderTextCardView.setVisibility(GONE);
                    holder.senderImageCardView.setVisibility(GONE);
                    holder.senderVideoCardView.setVisibility(GONE);

                    holder.receiverTextCardView.setVisibility(GONE);
                    holder.receiverVideoCardView.setVisibility(GONE);

                    holder.receiverImageCardView.setVisibility(VISIBLE);
                    holder.receiverMessageImageTime.setText(model.getTime());
                    holder.setReceiverMessageImage(model.getMessage());

                    holder.receiverMessageImage.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse("file://"+ model.getMessage()),"image/*");
                            context.startActivity(intent);
                        }
                    });
                    break;
                case "video":
                    holder.senderTextCardView.setVisibility(GONE);
                    holder.senderImageCardView.setVisibility(GONE);
                    holder.senderVideoCardView.setVisibility(GONE);

                    holder.receiverTextCardView.setVisibility(GONE);
                    holder.receiverImageCardView.setVisibility(GONE);

                    holder.receiverVideoCardView.setVisibility(VISIBLE);
                    holder.receiverMessageVideoTime.setText(model.getTime());
                    holder.receiverMessageVideoSize.setText(model.getSize());
                    holder.setReceiverMessageVideoImage(model.getVideo_thumb_url());

                    final String videoResult = getChatLocalStorageRef(context,model.getKey());

                    if (videoResult != null)
                    {
                        holder.receiverMessageVideoSize.setVisibility(GONE);
                        holder.receiverMessageVideoPlayBtn.setVisibility(VISIBLE);
                    }
                    else
                    {
                        holder.receiverMessageVideoPlayBtn.setVisibility(GONE);
                        holder.receiverMessageVideoSize.setVisibility(VISIBLE);
                    }

                    holder.receiverMessageVideoPlayBtn.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            File file = null;

                            if (videoResult != null)
                            {
                                file = new File(videoResult);
                            }

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                            context.startActivity(intent);
                        }
                    });

                    holder.receiverMessageVideoSize.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            if (!isDownloading)
                            {
                                Toast.makeText(context, "Video Downloading Started Please wait.... ", Toast.LENGTH_LONG).show();

                                holder.receiverMessageVideoSize.setVisibility(GONE);
                                holder.receiverMessageVideoDownloadingTextView.setVisibility(VISIBLE);

                                isDownloading = true;

                                notificationManagerCompat = NotificationManagerCompat.from(context);
                                builder = new NotificationCompat.Builder(context,CHAT_VIDEO_DOWNLOAD_CHANNEL_ID);
                                builder.setContentTitle("Chatting Video Download")
                                        .setSmallIcon(R.drawable.ic_file_download)
                                        .setPriority(NotificationCompat.PRIORITY_LOW)
                                        .setOngoing(true)
                                        .setAutoCancel(true);

                                StorageReference downloadRef = FirebaseStorage.getInstance().getReferenceFromUrl(model.getMessage());

                                final File localFile = new File(Environment.getExternalStorageDirectory() + "/ChatGoo/","Videos");

                                boolean created = false;

                                if (!localFile.exists())
                                {
                                    created = localFile.mkdirs();
                                }

                                if (created)
                                {
                                    final File downloadFile = new File(localFile,"VID_" + System.currentTimeMillis()  + ".mp4");

                                    downloadRef.getFile(downloadFile).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task)
                                        {
                                            builder.setContentText("Download Completed").setProgress(0, 0, false);
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());

                                            if (task.isSuccessful())
                                            {
                                                Toast.makeText(context, "Video Downloaded", Toast.LENGTH_SHORT).show();

                                                Uri file = Uri.fromFile(downloadFile);

                                                setChatLocalStorageRef(context,model.getKey(),file.toString());

                                                notificationManagerCompat.cancel(CHAT_VIDEO_DOWNLOAD_ID);
                                                builder = null;

                                                isDownloading = false;

                                                holder.receiverMessageVideoDownloadingTextView.setVisibility(GONE);
                                                holder.receiverMessageVideoPlayBtn.setVisibility(VISIBLE);

                                                holder.receiverMessageVideoPlayBtn.setOnClickListener(new View.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(View v)
                                                    {
                                                        if (videoResult != null)
                                                        {
                                                            File file = new File(videoResult);

                                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                                                            context.startActivity(intent);
                                                        }
                                                        else
                                                        {
                                                            final String result = getChatLocalStorageRef(context,model.getKey());

                                                            File file = null;

                                                            if (result != null)
                                                            {
                                                                file = new File(result);
                                                            }

                                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                                            intent.setDataAndType(Uri.fromFile(file),"video/*");
                                                            context.startActivity(intent);
                                                        }
                                                    }
                                                });
                                            }

                                            if (task.isCanceled())
                                            {
                                                isDownloading = false;

                                                Toast.makeText(context, "Downloading Cancelled", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener()
                                    {
                                        @Override
                                        public void onFailure(@NonNull Exception e)
                                        {
                                            builder.setContentTitle("Downloading failed").setOngoing(false);
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());

                                            isDownloading = false;

                                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>()
                                    {
                                        @Override
                                        public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot)
                                        {
                                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                                            builder.setProgress(100, (int) progress, false).setContentInfo((int) progress + "%").setContentText(getStringSizeFromFile(taskSnapshot.getBytesTransferred()) + " / " + getStringSizeFromFile(taskSnapshot.getTotalByteCount()));
                                            notificationManagerCompat.notify(CHAT_VIDEO_DOWNLOAD_ID, builder.build());
                                        }
                                    });
                                }

                            }
                            else
                            {
                                Toast.makeText(context, "Please wait another downloading is process..!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return friendsMessages.size();
    }

    class FriendsChattingViewHolder extends RecyclerView.ViewHolder
    {
        private CircleImageView thumb;
        private AppCompatTextView receiverMessageText,receiverMessageTextTime,senderMessageText,senderMessageTextTime,senderMessageImageTime,senderMessageVideoTime,senderMessageVideoSize,senderMessageVideoDownloadingTextView,receiverMessageImageTime,receiverMessageVideoTime,receiverMessageVideoSize,receiverMessageVideoDownloadingTextView;
        private AppCompatImageView senderMessageVideoPlayBtn,receiverMessageVideoPlayBtn,senderMessageImage,receiverMessageImage;
        private CardView senderTextCardView,senderImageCardView,senderVideoCardView,receiverTextCardView,receiverImageCardView,receiverVideoCardView;

        private View view;

        FriendsChattingViewHolder(View itemView)
        {
            super(itemView);

            view = itemView;

            thumb = view.findViewById(R.id.message_receiver_circle_image_view);
            senderMessageText = view.findViewById(R.id.sender_message_text);
            senderMessageTextTime = view.findViewById(R.id.sender_message_text_time);
            senderMessageImage = view.findViewById(R.id.sender_message_image);
            senderMessageImageTime = view.findViewById(R.id.sender_message_image_time);
            senderMessageVideoTime = view.findViewById(R.id.sender_message_video_time);
            senderMessageVideoSize = view.findViewById(R.id.sender_message_video_size);
            senderMessageVideoDownloadingTextView = view.findViewById(R.id.sender_message_video_downloading_text_view);
            senderMessageVideoPlayBtn = view.findViewById(R.id.sender_message_video_play_btn);
            receiverMessageText = view.findViewById(R.id.receiver_message_text);
            receiverMessageTextTime = view.findViewById(R.id.receiver_message_text_time);
            receiverMessageImage = view.findViewById(R.id.receiver_message_image);
            receiverMessageImageTime = view.findViewById(R.id.receiver_message_image_time);
            receiverMessageVideoTime = view.findViewById(R.id.receiver_message_video_time);
            receiverMessageVideoSize = view.findViewById(R.id.receiver_message_video_size);
            receiverMessageVideoDownloadingTextView = view.findViewById(R.id.receiver_message_video_downloading_text_view);
            receiverMessageVideoPlayBtn = view.findViewById(R.id.receiver_message_video_play_btn);
            senderTextCardView = view.findViewById(R.id.sender_text_card_view);
            senderImageCardView = view.findViewById(R.id.sender_image_card_view);
            senderVideoCardView = view.findViewById(R.id.sender_video_card_view);
            receiverTextCardView = view.findViewById(R.id.receiver_text_card_view);
            receiverImageCardView = view.findViewById(R.id.receiver_image_card_view);
            receiverVideoCardView = view.findViewById(R.id.receiver_video_card_view);
        }

        private void setReceiverProfileImage(final String thumbImg)
        {
            final CircleImageView thumb = view.findViewById(R.id.message_receiver_circle_image_view);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }

        private void setSenderMessageImage(final String thumbImg)
        {
            final AppCompatImageView thumb = view.findViewById(R.id.sender_message_image);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }

        private void setSenderMessageVideoImage(final String thumbImg)
        {
            final AppCompatImageView thumb = view.findViewById(R.id.sender_message_video);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }

        private void setReceiverMessageImage(final String thumbImg)
        {
            final AppCompatImageView thumb = view.findViewById(R.id.receiver_message_image);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }

        private void setReceiverMessageVideoImage(final String thumbImg)
        {
            final AppCompatImageView thumb = view.findViewById(R.id.receiver_message_video);

            Picasso.with(context).load(thumbImg).networkPolicy(NetworkPolicy.OFFLINE).into(thumb, new Callback()
            {
                @Override
                public void onSuccess()
                {

                }

                @Override
                public void onError()
                {
                    Picasso.with(context).load(thumbImg).placeholder(R.drawable.img_sel).into(thumb);
                }
            });
        }
    }

    private String getChatLocalStorageRef(Context context, String key)
    {
        String get = null;
        String result;

        SharedPreferences postPreference = context.getSharedPreferences("chats_storage_ref",MODE_PRIVATE);

        if (postPreference != null)
        {
            get = postPreference.getString(key,null);
        }

        if (get == null)
        {
            result = null;
        }
        else
        {
            result = get;
        }

        return result;
    }

    private void setChatLocalStorageRef(Context context,String key, String path)
    {
        SharedPreferences preferences = context.getSharedPreferences("chats_storage_ref",MODE_PRIVATE);

        if (preferences != null)
        {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key,path);
            editor.apply();
        }
    }
}
