package com.example.queueless;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.*;

public class activity_tokenstatus extends AppCompatActivity {

    TextView txtPosition, txtTime;
    DatabaseReference queueRef;
    int userToken = 10; // Example user token
    boolean notified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokenstatus);

        txtPosition = findViewById(R.id.txtPosition);
        txtTime = findViewById(R.id.txtTimeEstimate);

        queueRef = FirebaseDatabase.getInstance().getReference("queue");

        createNotificationChannel();

        queueRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                long current = snapshot.child("currentToken").getValue(Long.class);
                long avg = snapshot.child("avgTime").getValue(Long.class);

                int position = userToken - (int)current;
                int estimate = position * (int)avg;

                txtPosition.setText("Your Position: " + position);
                txtTime.setText("Estimated Time: " + estimate + " mins");

                if(position == 2 && !notified){
                    sendNotification();
                    notified = true;
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private void sendNotification(){
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this,"channel1")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Queue Alert")
                        .setContentText("Only 2 tokens ahead!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(this).notify(1,builder.build());
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel =
                    new NotificationChannel("channel1",
                            "Queue Channel",
                            NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
