package com.deffe.max.chatgoo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.deffe.max.chatgoo.Utils.NetworkStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.rilixtech.CountryCodePicker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity
{
    private CountryCodePicker countryCodePicker;
    private EditText userMobileNumberEditText,userReceivedOTPEditText;
    private Button sendOTPBtn,verifyOTPBtn,resendOTPBtn;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private PhoneAuthProvider.ForceResendingToken token;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference registerUserRef;

    private String phoneCode,countryName,userMobileNumber,verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        registerUserRef = FirebaseDatabase.getInstance().getReference().child("Users");

        countryCodePicker = findViewById(R.id.user_country_picker);
        userMobileNumberEditText = findViewById(R.id.user_mobile_number_editext);
        userReceivedOTPEditText = findViewById(R.id.user_received_otp_edittext);
        sendOTPBtn = findViewById(R.id.send_otp_code_btn);
        verifyOTPBtn = findViewById(R.id.otp_code_verify_btn);
        resendOTPBtn = findViewById(R.id.resend_otp_code_btn);

        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String userDefaultCountry = manager != null ? manager.getSimCountryIso() : null;

        countryCodePicker.setCountryPreference(userDefaultCountry);
        countryCodePicker.setCountryForNameCode(userDefaultCountry);
        countryCodePicker.setDefaultCountryUsingNameCode(userDefaultCountry);
        countryCodePicker.registerPhoneNumberTextView(userMobileNumberEditText);

        sendOTPBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String number = userMobileNumberEditText.getText().toString();

                if (TextUtils.isEmpty(number))
                {
                    Toast.makeText(LoginActivity.this, "Please enter valid mobile number", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (NetworkStatus.isConnected(LoginActivity.this) && NetworkStatus.isConnectedFast(LoginActivity.this))
                    {
                        String nameCode = countryCodePicker.getSelectedCountryNameCode();

                        phoneCode = countryCodePicker.getSelectedCountryCodeWithPlus();
                        countryName = countryCodePicker.getSelectedCountryName();
                        userMobileNumber = number.replaceAll("\\s+","");

                        countryCodePicker.setDefaultCountryUsingNameCode(nameCode);
                        countryCodePicker.setCountryPreference(nameCode);
                        countryCodePicker.setCountryForNameCode(nameCode);
                        countryCodePicker.registerPhoneNumberTextView(userMobileNumberEditText);
                        countryCodePicker.resetToDefaultCountry();

                        sentOTPToNumber(phoneCode,userMobileNumber);
                    }
                    else
                    {
                        Snackbar.make(findViewById(R.id.login_activity),"No Internet Connection", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

        verifyOTPBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String code = userReceivedOTPEditText.getText().toString();

                if (TextUtils.isEmpty(code))
                {
                    Toast.makeText(LoginActivity.this, "Please enter otp", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (NetworkStatus.isConnected(LoginActivity.this))
                    {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,code);

                        signInWithPhoneNumber(credential);
                    }
                    else
                    {
                        Snackbar.make(findViewById(R.id.login_activity),"No Internet Connection",Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });

        resendOTPBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (NetworkStatus.isConnected(LoginActivity.this))
                {
                    resendOTP();
                }
                else
                {
                    Snackbar.make(findViewById(R.id.login_activity),"No Internet Connection",Snackbar.LENGTH_LONG).show();
                }
            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks()
        {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential)
            {

            }

            @Override
            public void onVerificationFailed(FirebaseException e)
            {
                Toast.makeText(LoginActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken)
            {
                super.onCodeSent(s, forceResendingToken);

                verificationId = s;
                token = forceResendingToken;

                countryCodePicker.setVisibility(View.GONE);
                userMobileNumberEditText.setVisibility(View.GONE);
                sendOTPBtn.setVisibility(View.GONE);

                userReceivedOTPEditText.setVisibility(View.VISIBLE);
                verifyOTPBtn.setVisibility(View.VISIBLE);
                resendOTPBtn.setVisibility(View.VISIBLE);
            }
        };
    }
    private void resendOTP()
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneCode + userMobileNumber , 60, TimeUnit.SECONDS, LoginActivity.this , callbacks, token);
    }

    private void sentOTPToNumber(String phoneCode, String userMobileNumber)
    {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneCode + userMobileNumber , 60, TimeUnit.SECONDS, LoginActivity.this , callbacks);
    }

    private void signInWithPhoneNumber(PhoneAuthCredential credential)
    {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>()
        {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                if (task.isSuccessful())
                {
                    Toast.makeText(LoginActivity.this, "Verification Completed", Toast.LENGTH_SHORT).show();

                    FirebaseUser firebaseUser = task.getResult().getUser();

                    final String userId = firebaseUser.getUid();

                    FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>()
                    {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task)
                        {
                            if (task.isSuccessful())
                            {
                                final String deviceToken = task.getResult().getToken();

                                registerUserRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener()
                                {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                                    {
                                        if (dataSnapshot.getValue() != null)
                                        {
                                            Map<String, Object> update = new HashMap<>();

                                            update.put("device_token",deviceToken);
                                            update.put("status","active");

                                            registerUserRef.child(userId).updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if (task.isSuccessful())
                                                    {
                                                        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
                                                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(mainIntent);
                                                        finish();
                                                    }
                                                }
                                            });
                                        }
                                        else
                                        {
                                            Map<String, Object> register = new HashMap<>();

                                            register.put("user_id",userId);
                                            register.put("device_token",deviceToken);
                                            register.put("user_number",userMobileNumber);
                                            register.put("user_number_with_plus", phoneCode+userMobileNumber);
                                            register.put("country",countryName);
                                            register.put("time", ServerValue.TIMESTAMP);
                                            register.put("user_profile_img","default_profile_img");
                                            register.put("user_profile_thumb_img","default_profile_thumb_img");
                                            register.put("status","active");

                                            registerUserRef.child(userId).updateChildren(register).addOnCompleteListener(new OnCompleteListener<Void>()
                                            {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task)
                                                {
                                                    if (task.isSuccessful())
                                                    {
                                                        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
                                                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        startActivity(mainIntent);
                                                        finish();
                                                    }
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError)
                                    {
                                        Toast.makeText(LoginActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });

                    registerUserRef.child(userId).addValueEventListener(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            if (dataSnapshot.exists())
                            {

                            }
                            else
                            {

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError)
                        {
                            Toast.makeText(LoginActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(LoginActivity.this, e.getMessage() , Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null)
        {
            Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        }
    }
}
