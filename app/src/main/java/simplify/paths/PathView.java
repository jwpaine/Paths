package simplify.paths;



/**
 * Created by tsunami on 6/1/17.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
        import android.view.View;

import java.util.List;

public class PathView extends View {

    private List<List> path;
    private Map ui;
    private boolean run;

    public PathView(Context cxt, Map ui) {
        super(cxt);
        setMinimumHeight(300);
        setMinimumWidth(100);
        this.ui = ui;
        run = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        Paint p = new Paint();
        p.setColor(Color.GREEN);
        p.setStrokeWidth(5);

        canvas.save();


        path = ui.getPath();

        for (int i = 0; i < path.size(); i++) {

            if (path.get(i).get(0) == null) continue;

            canvas.drawCircle((float) (this.getHeight() / 2 + (double) path.get(i).get(0)), (float) (this.getWidth() / 2 + (double) path.get(i).get(1)), 5, p);

            System.out.println("(" + String.valueOf(path.get(i).get(0)) + "," + String.valueOf(path.get(i).get(1) ));
        }







   canvas.restore();
        invalidate();






    }

    /* pass reference to path */
    public void connectPath(List<List> path) {
        this.path = path;
        run = true;

    }
}