package org.iget.org.elasticsearch;

import java.text.BreakIterator;
import java.util.Locale;

public class test {
    public static void printEachForward(BreakIterator boundary, String source) {
        int start = boundary.first();
        for (int end = boundary.next();
             end != BreakIterator.DONE;
             start = end, end = boundary.next()) {
            System.out.println(source.substring(start,end));
        }
    }
    public static void printEachBackward(BreakIterator boundary, String source) {
        int end = boundary.last();
        for (int start = boundary.previous();
             start != BreakIterator.DONE;
             end = start, start = boundary.previous()) {
            System.out.println(source.substring(start,end));
        }
    }
    public static void printFirst(BreakIterator boundary, String source) {
        int start = boundary.first();
        int end = boundary.next();
        System.out.println(source.substring(start,end));
    }
    public static void printLast(BreakIterator boundary, String source) {
        int end = boundary.last();
        int start = boundary.previous();
        System.out.println(source.substring(start,end));
    }
    public static void main(String args[]) {

            String stringToExamine = "比如，在中国，写小说的人，没人敢说自己会超越《红楼梦》；写古体诗的，没有人会认为自己比李白和杜甫还好；写书法，没人敢说自己比王羲之、颜真卿、柳公权更好。 但是， 如果没有创新，一个艺术家，就找不到自己的存在感，也很难对这门艺术做出自己的贡献。";
            //print each word in order
            BreakIterator boundary = BreakIterator.getCharacterInstance();
            boundary.setText(stringToExamine);
            printEachForward(boundary, stringToExamine);
            //print each sentence in reverse order
            boundary = BreakIterator.getCharacterInstance(Locale.CHINESE);
            boundary.setText(stringToExamine);
            printEachBackward(boundary, stringToExamine);
//            printFirst(boundary, stringToExamine);
//            printLast(boundary, stringToExamine);

    }
}
