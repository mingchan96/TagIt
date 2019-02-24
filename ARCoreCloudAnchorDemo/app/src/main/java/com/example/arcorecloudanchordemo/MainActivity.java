package com.example.arcorecloudanchordemo;

import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;

public class MainActivity extends AppCompatActivity {

    private CustomArFragment arFragment;

    //check for ARCore id if it is being hosting
    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }

    private Anchor anchor;
    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    private boolean isPlaced = false;
    //testing on same device, used shared preferences for the anchor id
    //SharedPreferences points to a key-value pairs file
    private SharedPreferences prefs;
    //able to make changes to the SharedPreferences
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AnchorId", MODE_PRIVATE);
        editor = prefs.edit();

        arFragment = (CustomArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        arFragment.setOnTapArPlaneListener(((hitResult, plane, motionEvent) -> {

            //if the model is placed then don't place model again/user's tap
            if(!isPlaced) {
                anchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                appAnchorState = AppAnchorState.HOSTING;

                showToast("Hosting....");

                createModel(anchor);
                isPlaced = true;
            }
        }));

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if(appAnchorState != AppAnchorState.HOSTING)
                return;

            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();

            //check if there is an error in hosting the cloud Anchor
            if(cloudAnchorState.isError()){
                showToast(cloudAnchorState.toString());
            }
            else if(cloudAnchorState == Anchor.CloudAnchorState.SUCCESS){
                appAnchorState = AppAnchorState.HOSTED;

                //get the id associated with the anchor
                //the id is a long string so use a shorter key
                String anchorId = anchor.getCloudAnchorId();
                editor.putString("anchorId",anchorId);
                editor.apply();

                showToast("Anchor hosted successfully. Anchor id: " + anchorId);
            }
        });

        Button resolve = findViewById(R.id.resolve);
        resolve.setOnClickListener(view -> {
            String anchorId = prefs.getString("anchorId", "null");

            if (anchorId.equals("null")){
                Toast.makeText(this, "No anchorId found", Toast.LENGTH_LONG).show();
                return;
            }

            Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(anchorId);
            createModel(resolvedAnchor);

        });

    }

    private void showToast(String s) {
        Toast.makeText(this,s, Toast.LENGTH_LONG).show();
    }

    //ArcticFox_Posed.sfb
    //Giraffe_01(1).sfb
    private void createModel(Anchor anchor) {
        ModelRenderable
                .builder()
                .setSource(this, Uri.parse("ArcticFox_Posed.sfb"))
                .build()
                .thenAccept(modelRenderable -> placeModel(anchor, modelRenderable));
    }

    private void placeModel(Anchor anchor, ModelRenderable modelRenderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }
}
