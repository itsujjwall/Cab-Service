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

public class CustomerLoginActivity extends AppCompatActivity {
    private TextInputEditText CEmailInput,CPasswordInput;
    private Button CLoginBtn,CSignUpBtn;
    private TextView CForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);


        mAuth=FirebaseAuth.getInstance();
        authStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();

                if (user!=null){
                    Intent intent=new Intent(CustomerLoginActivity.this,CustomerMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };


        //init
        CEmailInput=findViewById(R.id.cemail_inp);
        CPasswordInput=findViewById(R.id.cpassword_inp);
        CLoginBtn=findViewById(R.id.customer_login_btn);
        CSignUpBtn=findViewById(R.id.customer_creating_account_btn);
        CForgotPassword=findViewById(R.id.cforgot_password);



        CSignUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email= Objects.requireNonNull(CEmailInput.getText()).toString();
                final String password= Objects.requireNonNull(CPasswordInput.getText()).toString();

                mAuth.createUserWithEmailAndPassword(email,password).
                        addOnCompleteListener(CustomerLoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){
                                    Toast.makeText(CustomerLoginActivity.this, "Customer SignUp Successfully...", Toast.LENGTH_SHORT).show();

                                    String user_id= Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

                                    DatabaseReference current_user_db= FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(user_id);
                                    current_user_db.setValue(true);

                                }
                                else {
                                    Toast.makeText(CustomerLoginActivity.this, "Error Occur: Try Again...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(CustomerLoginActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CustomerLoginActivity.this, "Sign up Error Occur: "+e.getMessage()+"\n Try Again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        CLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email= Objects.requireNonNull(CEmailInput.getText()).toString();
                final String password= Objects.requireNonNull(CPasswordInput.getText()).toString();

                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(CustomerLoginActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()){

                                }
                                else{
                                    Toast.makeText(CustomerLoginActivity.this, "Login Error Occur: Try Again...", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(CustomerLoginActivity.this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CustomerLoginActivity.this, "Error Occur: "+e.getMessage()+"\n Try Again.", Toast.LENGTH_SHORT).show();
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