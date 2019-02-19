package com.example.david_ar_demo;

import android.net.Uri;
import android.os.TransactionTooLargeException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        //whenever the user taps on the frame, it creates an anchor on the plane
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            //anchor is used to describe a fix location or location in the real world
            Anchor anchor = hitResult.createAnchor();

            //creates the model
            ModelRenderable.builder()
                    .setSource(this, Uri.parse("ArcticFox_Posed.sfb"))
                    .build()
                    .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                    .exceptionally(throwable -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage(throwable.getMessage()).show();
                        return null;
                    });
        });

    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        //automatically position itself based on anchor (created from hitResult (setOnTapArPlaneListener))
        AnchorNode anchorNode = new AnchorNode(anchor);
        //allow for zooming (increasing/decreasing the anchor's size)
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(modelRenderable);
        //place the anchor on the scene
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }
}
