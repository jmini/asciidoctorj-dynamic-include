package fr.jmini.asciidoctorj.dynamicinclude.config;

public enum Order {
    /**
     * sorts in alphabetical order of their component letters
     */
    LEXICOGRAPHIC,
    /**
     * sorts in the reversed order of {@link #LEXICOGRAPHIC}.
     */
    LEXICOGRAPHIC_REVERSED,
    /**
     * sorts strings containing a mix of letters and numbers. Given strings of mixed characters and numbers, it sorts the numbers in value order, while sorting the non-numbers in ASCII order.
     */
    NATURAL,
    /**
     * sorts strings containing a mix of letters and numbers in the reversed order of {@link #NATURAL}.
     */
    NATURAL_REVERSED

}
