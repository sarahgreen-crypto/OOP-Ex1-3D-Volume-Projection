package engine;

public interface PlaneDefinition {
    int width();
    int height();

    // מספר שורות ורוחב, לפי SRS סעיף 2.4
    double length();                       // L: אורך השוק

    // נקודת ההתחלה של כל צד (start1 ו-start2)
    double start1X(); double start1Y(); double start1Z();
    double start2X(); double start2Y(); double start2Z();

    // וקטורי הכיוון (יחידה) לכל צד
    double dir1X(); double dir1Y(); double dir1Z();
    double dir2X(); double dir2Y(); double dir2Z();
}