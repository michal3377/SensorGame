package com.lab13.sensorgame;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static final long REFRESH_DELAY = 50; //milisekundy

    // obiekty odpowiedzialne za sensor polozenia
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorListener;

    // zmienne odpowiedzialne za stan naszej gry
    private int xMin, xMax; // zakres naszego ekranu, do sprawdzania czy kulka nie wypadła
    private int yMin, yMax;

    private int ballWidth, ballHeight; //rozmiar kulki
    private float ballX, ballY; //aktualne wspolrzedne kulki

    private double forceX, forceY; // aktualna "siła" dzialajaca na oś, w zależności od przechylenia
    private double speedVector = 1.0; // mnożnik prędkości który możemy sobie zmieniać żeby kulka szybciej/wolniej latała
    private boolean gameStarted = false; // bool, czy gra jest w trakcie czy zatrzymana


    private Handler handler; // Androidowa klasa Handler, służy do wywoływania funkcji po jakimś określonym czasie
    private Runnable gameTick; // To jest ta nasza "funkcja" jako interfejs (coś jak listenery), którą Handler będzie wykonywał po czasie

    private View ball; // Widok naszej kulki, aktualnie w XML'u jest to TextView ale mozecie zmienic na cokolwiek (wszystkie widoki w Androidzie dziedziczą z View więc nie trzeba zmieniać tutaj typu)
    private Button btStart; //przycisk od startowania gry
    private ViewGroup ltGame; //Layout po którym kulka jeździ, aktualnie w XML'u jest to domyślny ConstraintLayout który wypełnia cały ekran




    @Override
    protected void onCreate(Bundle savedInstanceState) { //metoda onCreate, to taki "konstruktor" Activity, wykonuje się na początku i tutaj wszystko inicjalizujemy
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //ładujemy XML
        getSupportActionBar().hide(); // chowamy Toolbar, ten górny pasek

        ball = findViewById(R.id.ball); // przypisujemy XML'owe widoki do naszych zmiennych, za pomocą ich id
        btStart = findViewById(R.id.btStart);
        ltGame = findViewById(R.id.ltGame);

        handler = new Handler();
        btStart.setOnClickListener(view -> startGame()); //event kliknięcia przycisku
        gameTick = () -> { //to jest nasza funkcja "odświeżenia" gry, która będzie wykonywana co 50 milisekund (20 razy na sekunde)
            if (!gameStarted) return;
            ballX += forceX * speedVector; //zwiekszamy/zmniejszamy współrzędne piłki o wartość ostatniego odczytu z sensora "nachylenia" telefonu
            ballY += forceY * speedVector;
            ball.setX(ballX); //ustawiamy współrzędne piłki na te nasze zaktualizowane
            ball.setY(ballY);
            if (isBallOutOfBounds()) { //sprawdzamy czy piłka nie wyjechała poza ekran
                finishGame();
            } else {
                // "Zlecamy" wykonanie tej funkcji gameTick jeszcze raz, za 50 milisekund, więc dopóki piłka nie wyleci poza ekran
                //  to ta funkcja będzie (poniekąd) wykonywać samą siebie co 50 ms. Powoduje to że gra jest w miarę płynna i responsywna
                handler.postDelayed(gameTick, REFRESH_DELAY);
            }
        }; //koniec gameTick

        initSensor();
    }

    void initSensor() {
        // inicjalizacja sensora Akcelerometru. Ctrl+V z dokumentacji
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorListener = new SensorEventListener() {
            @Override
            //to nasz event zaktualizowania odczytów nachylenia telefonu, wykonuje sie ~50 razy na sekunde
            public void onSensorChanged(SensorEvent sensorEvent) {
                forceX = -sensorEvent.values[0]; // oś X, minusik bo ta oś na odwrót
                forceY = sensorEvent.values[1];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    private void startGame() {
        ballX = 300;
        ballY = 300;
        xMin = 0;
        yMin = 0;
        //ważna Androidowa sprawa, w onCreate nie możemy pobierać żadnych współrzędnych ani wymiarów widoków bo widoki jeszcze nie są ulokowane i te metody zwracają 0
        // tak więc tutaj najprościej pobierać te wymiary po kliknięciu "Start", wtedy mamy pewność że cały layout jest ulokowany
        xMax = ltGame.getWidth(); //prawa krawedz pola gry ma wspolrzedne rowne szerokosci naszego layoutu, bo wypełnia cały ekran
        yMax = ltGame.getHeight();
        ballWidth = ball.getWidth();
        ballHeight = ball.getHeight();
        btStart.setVisibility(View.INVISIBLE);
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME); //"Włączamy" sensor żeby zaczął nam podawać aktualne wartości
        handler.postDelayed(gameTick, REFRESH_DELAY); // uruchamiamy naszą "pętle" gry, gameTick
        gameStarted = true;
    }

    private void finishGame() {
        btStart.setVisibility(View.VISIBLE);
        sensorManager.unregisterListener(sensorListener);
        handler.removeCallbacks(gameTick);
        gameStarted = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishGame(); //onPause wykonuje sie gdy użytkownik np. przejdzie do innego ekranu/zminimalizuje apke
    }

    private boolean isBallOutOfBounds() {
        return ballX < xMin || ballX + ballWidth > xMax
                || ballY < yMin || ballY + ballHeight > yMax;
    }
}
