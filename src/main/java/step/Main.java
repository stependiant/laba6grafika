package step;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Основной класс, реализующий алгоритм Сазерленда-Коэна
 * для отсечения отрезков и их визуализации.
 */
public class Main extends JPanel {
    // Границы окна
    double xLeft, yTop, xRight, yBottom;

    // Исходные отрезки
    List<LineSegment> originalSegments = new ArrayList<>();

    // Отсеченные отрезки
    List<LineSegment> clippedSegments = new ArrayList<>();

    // Константы для кодирования положения точки относительно окна:
    // Биты: 1-й - левее, 2-й - выше, 3-й - ниже, 4-й - правее
    static final int LEFT_BIT = 1;   // 0001
    static final int TOP_BIT = 2;   // 0010
    static final int BOTTOM_BIT = 4;   // 0100
    static final int RIGHT_BIT = 8;   // 1000

    public Main(double xLeft, double yTop, double xRight, double yBottom, List<LineSegment> segments) {
        this.xLeft = xLeft;
        this.yTop = yTop;
        this.xRight = xRight;
        this.yBottom = yBottom;
        this.originalSegments = segments;
        clipAllSegments();
    }

    // Метод для вычисления кода конца отрезка
    private int computeOutCode(double x, double y) {
        int code = 0;
        if (x < xLeft) code |= LEFT_BIT;
        if (x > xRight) code |= RIGHT_BIT;
        if (y < yBottom) code |= BOTTOM_BIT;
        if (y > yTop) code |= TOP_BIT;
        return code;
    }

    // Алгоритм Сазерленда-Коэна для одного отрезка
    private LineSegment cohenSutherlandClip(LineSegment seg) {
        double x0 = seg.x1, y0 = seg.y1;
        double x1 = seg.x2, y1 = seg.y2;

        int outCode0 = computeOutCode(x0, y0);
        int outCode1 = computeOutCode(x1, y1);
        boolean accept = false;

        while (true) {
            if ((outCode0 | outCode1) == 0) {
                // Отрезок полностью внутри окна
                accept = true;
                break;
            } else if ((outCode0 & outCode1) != 0) {
                // Отрезок полностью вне окна
                break;
            } else {
                // Частичное пересечение
                double x = 0, y = 0;
                int outCodeOut = (outCode0 != 0) ? outCode0 : outCode1;

                if ((outCodeOut & TOP_BIT) != 0) {
                    x = x0 + (x1 - x0) * (yTop - y0) / (y1 - y0);
                    y = yTop;
                } else if ((outCodeOut & BOTTOM_BIT) != 0) {
                    x = x0 + (x1 - x0) * (yBottom - y0) / (y1 - y0);
                    y = yBottom;
                } else if ((outCodeOut & RIGHT_BIT) != 0) {
                    y = y0 + (y1 - y0) * (xRight - x0) / (x1 - x0);
                    x = xRight;
                } else if ((outCodeOut & LEFT_BIT) != 0) {
                    y = y0 + (y1 - y0) * (xLeft - x0) / (x1 - x0);
                    x = xLeft;
                }

                // Обновляем конец отрезка
                if (outCodeOut == outCode0) {
                    x0 = x;
                    y0 = y;
                    outCode0 = computeOutCode(x0, y0);
                } else {
                    x1 = x;
                    y1 = y;
                    outCode1 = computeOutCode(x1, y1);
                }
            }
        }

        if (accept) {
            return new LineSegment(x0, y0, x1, y1);
        } else {
            return null;
        }
    }

    // Отсечение всех отрезков
    private void clipAllSegments() {
        clippedSegments.clear();
        for (LineSegment seg : originalSegments) {
            LineSegment clipped = cohenSutherlandClip(seg);
            if (clipped != null) {
                clippedSegments.add(clipped);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Смещение/масштаб для удобства отображения (опционально)
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        // Определим масштаб, чтобы уместить всё в окно
        int width = getWidth();
        int height = getHeight();

        // Минимальный масштаб:
        double minX = Math.min(xLeft, xRight);
        double maxX = Math.max(xLeft, xRight);
        double minY = Math.min(yBottom, yTop);
        double maxY = Math.max(yBottom, yTop);

        double scaleX = width / (maxX - minX + 1);
        double scaleY = height / (maxY - minY + 1);
        double scale = Math.min(scaleX, scaleY);

        // Центрирование
        double xOffset = -minX;
        double yOffset = -minY;

        // Функция преобразования координат
        // из математических в координаты экрана
        // screenX = (x + xOffset)*scale
        // screenY = height - (y + yOffset)*scale (инвертируем ось Y для удобства)

        // Рисуем окно
        g.setColor(Color.RED);
        int wx1 = (int) ((xLeft + xOffset) * scale);
        int wy1 = (int) (height - (yTop + yOffset) * scale);
        int wwidth = (int) ((xRight - xLeft) * scale);
        int wheight = (int) ((yTop - yBottom) * scale);
        // Примечание: вычитаем yTop, чтобы верх окна был выше на экране
        g.drawRect(wx1, wy1, wwidth, wheight);

        // Рисуем исходные отрезки серым цветом
        g.setColor(Color.LIGHT_GRAY);
        for (LineSegment seg : originalSegments) {
            int sx1 = (int) ((seg.x1 + xOffset) * scale);
            int sy1 = (int) (height - (seg.y1 + yOffset) * scale);
            int sx2 = (int) ((seg.x2 + xOffset) * scale);
            int sy2 = (int) (height - (seg.y2 + yOffset) * scale);
            g.drawLine(sx1, sy1, sx2, sy2);
        }

        // Рисуем отсеченные отрезки чёрным цветом
        g.setColor(Color.BLACK);
        for (LineSegment seg : clippedSegments) {
            int sx1 = (int) ((seg.x1 + xOffset) * scale);
            int sy1 = (int) (height - (seg.y1 + yOffset) * scale);
            int sx2 = (int) ((seg.x2 + xOffset) * scale);
            int sy2 = (int) (height - (seg.y2 + yOffset) * scale);
            g.drawLine(sx1, sy1, sx2, sy2);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Чтение координат окна
        System.out.println("Введите координаты окна:");
        System.out.print("Левая граница (xLeft): ");
        double xLeft = sc.nextDouble();
        System.out.print("Верхняя граница (yTop): ");
        double yTop = sc.nextDouble();
        System.out.print("Правая граница (xRight): ");
        double xRight = sc.nextDouble();
        System.out.print("Нижняя граница (yBottom): ");
        double yBottom = sc.nextDouble();

        // Чтение отрезков
        System.out.print("Введите количество отрезков: ");
        int n = sc.nextInt();

        List<LineSegment> segments = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            System.out.println("Отрезок " + (i + 1) + ":");
            System.out.print("x1 = ");
            double x1 = sc.nextDouble();
            System.out.print("y1 = ");
            double y1 = sc.nextDouble();
            System.out.print("x2 = ");
            double x2 = sc.nextDouble();
            System.out.print("y2 = ");
            double y2 = sc.nextDouble();
            segments.add(new LineSegment(x1, y1, x2, y2));
        }

        sc.close();

        // Создание окна с визуализацией
        JFrame frame = new JFrame("Отсечение отрезков (Сазерленд-Коэн)");
        Main panel = new Main(xLeft, yTop, xRight, yBottom, segments);
        frame.add(panel);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}