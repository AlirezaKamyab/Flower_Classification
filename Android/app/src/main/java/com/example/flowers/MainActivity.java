package com.example.flowers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.flowers.ml.Efficientnet099;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    public final String[] LABELS = {"pink primrose", "hard-leaved pocket orchid", "canterbury bells",
            "sweet pea", "english marigold", "tiger lily", "moon orchid", "bird of paradise",
            "monkshood", "globe thistle", "snapdragon", "colt's foot", "king protea", "spear thistle",
            "yellow iris", "globe-flower", "purple coneflower", "peruvian lily", "balloon flower",
            "giant white arum lily", "fire lily", "pincushion flower", "fritillary", "red ginger",
            "grape hyacinth", "corn poppy", "prince of wales feathers", "stemless gentian", "artichoke",
            "sweet william", "carnation", "garden phlox", "love in the mist", "mexican aster",
            "alpine sea holly", "ruby-lipped cattleya", "cape flower", "great masterwort", "siam tulip",
            "lenten rose", "barbeton daisy", "daffodil", "sword lily", "poinsettia", "bolero deep blue",
            "wallflower", "marigold", "buttercup", "oxeye daisy", "common dandelion", "petunia",
            "wild pansy", "primula", "sunflower", "pelargonium", "bishop of llandaff", "gaura",
            "geranium", "orange dahlia", "pink-yellow dahlia?", "cautleya spicata", "japanese anemone",
            "black-eyed susan", "silverbush", "californian poppy", "osteospermum", "spring crocus",
            "bearded iris", "windflower", "tree poppy", "gazania", "azalea", "water lily", "rose",
            "thorn apple", "morning glory", "passion flower", "lotus", "toad lily", "anthurium",
            "frangipani", "clematis", "hibiscus", "columbine", "desert-rose", "tree mallow", "magnolia",
            "cyclamen ", "watercress", "canna lily", "hippeastrum ", "bee balm", "ball moss", "foxglove",
            "bougainvillea", "camellia", "mallow", "mexican petunia", "bromelia", "blanket flower",
            "trumpet creeper", "blackberry lily"};
    Bitmap bitmap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView txtSource = (TextView) findViewById(R.id.txtSource);
        txtSource.setMovementMethod(LinkMovementMethod.getInstance());

        Button btnChoose = (Button) findViewById(R.id.btnChoose);
        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenGallary();
            }
        });


        Button btnClassify = (Button) findViewById(R.id.btnClassify);
        btnClassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                    tensorImage.load(bitmap);

                    // Process Image
                    ImageProcessor imageProcessor =
                            new ImageProcessor.Builder()
                                    .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                                    .build();

                    tensorImage = imageProcessor.process(tensorImage);

                    TensorBuffer buffer = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                    buffer.loadBuffer(tensorImage.getBuffer());

                    Log.d("ML", Arrays.toString(buffer.getShape()));

                    Efficientnet099 model = Efficientnet099.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
                    inputFeature0.loadBuffer(tensorImage.getBuffer());

                    // Runs model inference and gets result.
                    Efficientnet099.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Releases model resources if no longer used.
                    model.close();

                    float[] floatArr = outputFeature0.getFloatArray();
                    int[] indicies = new int[102];
                    for(int i = 0; i < 102; i++) indicies[i] = i;

                    Log.d("ML", Arrays.toString(floatArr));

                    for (int i = 1; i < 102; i++) {
                        float key = floatArr[i];
                        int ikey = indicies[i];
                        int j = i - 1;
                        while(j >= 0 && key > floatArr[j]) {
                            floatArr[j + 1] = floatArr[j];
                            indicies[j + 1] = indicies[j];
                            j -= 1;
                        }
                        floatArr[j + 1] = key;
                        indicies[j + 1] = ikey;
                    }

                    for(int i = 0; i < 102; i++) Log.d("ML", String.format("%.2f %d", floatArr[i], indicies[i]));

                    TextView txtClass = (TextView) findViewById(R.id.txtClass);

                    String text = "";
                    for(int c = 0; c < 3; c++) {
                        float acc = floatArr[c];
                        int index = indicies[c];
                        if(acc * 100 < 1) break;
                        text = text + LABELS[index] + " %" + String.format("%.2f\n", acc * 100);
                    }
                    txtClass.setText(text);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Please choose an image first", Toast.LENGTH_SHORT)
                            .show();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private final ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri selectedImage = intent.getData();
                        if (selectedImage != null) {
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                                ImageView imageView = (ImageView) findViewById(R.id.imgView);
                                imageView.setImageBitmap(bitmap);

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
    );


    public void OpenGallary() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        mGetContent.launch(intent);
    }

}