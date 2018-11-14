# SignatureView

## Demo on Samsung Tab s4 with sPen

<img src="https://github.com/mgcoco/SignatureView/blob/master/screenshot/1.jpg" width="250px" />

## Gradle Dependency

```
allprojects {
    repositories {
      ...
      maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation 'com.github.mgcoco:SignatureView:v1.0.1'
}
```

## Basic

Xml File

```
<com.mgcoco.signatureview.SignatureView
    android:id="@+id/signature"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:msv_pen_color="@color/colorAccent
    app:msv_pen_thinkness="4" />
```

Java Code

```
 SignatureView signatureView = findViewById(R.id.signature);
 //Pen color
 signatureView.setColor(255, 0, 0);//RGB
 //Pen thinckness
 signatureView.setThickness(3);
 //Get signature
 Bitmap signature = signatureView.getSignature();
 //Clear signature view              
 signatureView.clearCanvas();
