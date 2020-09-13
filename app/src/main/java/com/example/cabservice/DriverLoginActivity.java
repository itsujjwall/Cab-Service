package com.example.cabservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class DriverLoginActivity extends AppCompatActivity {
    private TextInputEditText DEmailInput,DPasswordInput;
    private Button DLoginBtn,DSignUpBtn;
    private TextView DForgotPassword;


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth=FirebaseAuth.getInstance();
        authStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();

                if (user!=null){
                    Intent intent=new Intent(DriverLoginActivity.this,DriverMapsActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        //init
        DEmailInput=findViewById(R.id.email_inp);
        DPasswordInput=findViewById(R.id.password_inp);
        DLoginBtn=findViewById(R.id.driver_login_btn);
        DSignUpBtn=findViewById(R.id.driver_creating_account_btn);
        DForgotPassword=findViewById(R.id.dforgot_password);



        DSignUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email= Objects.requireNonNull(DEmailInput.getText()).toString();
                final String password= Objects.requireNonNull(DPasswordInput.getText()).toString();

                mAuth.createUserWithEmailAndPassword(email,password).
                        addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    Toast.makeText(DriverLoginActivity.this, "Driver SignUp Successfully...", Toast.LENGTH_SHORT).show();

                                    String user_id= Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

                                    DatabaseReference current_user_db= FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(user_id);
                                    current_user_db.setValue(true);

                                }
                                else {
                                    Toast.makeText(DriverLoginActivity.this, "Error Occur: Try Again...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(DriverLoginActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DriverLoginActivity.this, "Sign up Error Occur: "+e.getMessage()+"\n Try Again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        DLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email= Objects.requireNonNull(DEmailInput.getText()).toString();
                final String password= Objects.requireNonNull(DPasswordInput.getText()).toString();

                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(DriverLoginActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    
                                }
                                else{
                                    Toast.makeText(DriverLoginActivity.this, "Login Error Occur: Try Again...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(DriverLoginActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DriverLoginActivity.this, "Error Occur: "+e.getMessage()+"\n Try Again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }




    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);

    }

    @Override
    protected void onStop() {
        super.onStop();

        mAuth.removeAuthStateListener(authStateListener);

    }
}