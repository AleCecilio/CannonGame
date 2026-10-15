// MainActivity.java
// MainActivity exibe o MainActivityFragment (Fig. 6.5)
package com.deitel.cannongame;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
   // chamado quando o app é iniciado pela primeira vez
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
   }
}
