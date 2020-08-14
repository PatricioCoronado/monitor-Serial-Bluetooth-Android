package com.segainvex.mibluetooth;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
/****************************************************************************************
 *  Esta clase se encarga de procesar las respuestas recibidas de Arduino
*************************************************************************************** */
class Respuestas //No defino constructor, constructor por defecto
{
    private LinearLayout graficoZ;

    /*************************************************************************************
     * Procesa la respuesta del acelerómetro
    * ***********************************************************************************/
    public void acelerometro(byte[] respuesta, int largoRespuesta, TextView GX, TextView GY)
    {
        this.graficoZ = graficoZ;
        int indice;
        for(indice=0;indice < largoRespuesta-1;indice++)//Recorre el array de la respuesta...
        {
            if(respuesta[indice]==32)//Buscando el caracter espacio
            {   //Cuando lo encuentra ya puede leer el valor de x
                String gx=new String(Arrays.copyOfRange(respuesta, 0, indice));
                String gy=new String(Arrays.copyOfRange(respuesta, indice+1, largoRespuesta-1));
                GY.setText("y= "+gy);
                GX.setText("x= "+gx);
                //graficoZ.addView(grafico);
                break;//Salimos del for
            }
        }
    }
    /*************************************************************************************
     * Procesa la respuesta del fotodiodo
     * ***********************************************************************************/
    /*************************************************************************************
     * Procesa la respuesta de temperatura humedad
     * ***********************************************************************************/
     /*************************************************************************************
     * Procesa la respuesta de
     * ***********************************************************************************/
}
