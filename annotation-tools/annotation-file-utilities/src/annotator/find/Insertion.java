package annotator.find;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sun.tools.javac.code.TypeAnnotationPosition.TypePathEntry;

import plume.Pair;
import type.ArrayType;
import type.DeclaredType;
import type.Type;
import type.BoundedType;

/**
 * Specifies something that needs to be inserted into a source file, including
 * the "what" and the "where".
 */
public abstract class Insertion {

    public enum Kind {
        ANNOTATION,
        CAST,
        NEW,
        RECEIVER,
        CLOSE_PARENTHESIS
    }

    private final Criteria criteria;
    // If non-null, then try to put annotation on its own line,
    // horizontally aligned with the location.
    private final boolean separateLine;

    /**
     * Whether this insertion has already been inserted into source code. See
     * {@link ReceiverInsertion} for a better description of how this is used.
     */
    private boolean inserted;

    /**
     * The package names for the annotations being inserted by this Insertion.
     * This will be empty unless {@link #getText(boolean, boolean)} is called
     * with abbreviate true.
     */
    protected Set<String> packageNames;

    /**
     * Creates a new insertion.
     *
     * @param criteria where to insert the text
     * @param separateLine whether to insert the text on its own
     */
    public Insertion(Criteria criteria, boolean separateLine) {
        this.criteria = criteria;
        this.separateLine = separateLine;
        this.packageNames = new LinkedHashSet<String>();
        this.inserted = false;
    }

    /**
     * Gets the insertion criteria.
     *
     * @return the criteria
     */
    public Criteria getCriteria() {
        return criteria;
    }

    /**
     * Gets the insertion text (not commented or abbreviated, and without added
     * leading or trailing whitespace).
     *
     * @return the text to insert
     */
    public String getText() {
        return getText(false, false, true, 0, '\0');
    }

    /**
     * Gets the insertion text with a leading and/or trailing space added based
     * on the values of the {@code gotSeparateLine}, {@code pos}, and
     * {@code precedingChar} parameters.
     *
     * @param comments
     *            if true, Java 8 features will be surrounded in comments.
     * @param abbreviate
     *            if true, the package name will be removed from the annotations.
     *            The package name can be retrieved again by calling the
     *            {@link #getPackageNames()} method.
     * @param gotSeparateLine
     *            {@code true} if this insertion is actually added on a separate
     *            line.
     * @param pos
     *            the source position where this insertion will be inserted.
     * @param precedingChar
     *            the character directly preceding where this insertion will be
     *            inserted. This value will be ignored if {@code pos} is 0.
     *
     * @return the text to insert
     */
    public String getText(boolean comments, boolean abbreviate,
            boolean gotSeparateLine, int pos, char precedingChar) {
        String toInsert = getText(comments, abbreviate);
        if (addLeadingSpace(gotSeparateLine, pos, precedingChar)) {
            toInsert = " " + toInsert;
        }
        if (addTrailingSpace(gotSeparateLine)) {
            toInsert = toInsert + " ";
        }
        return toInsert;
    }

    /**
     * Gets the insertion text.
     *
     * @param comments
     *            if true, Java 8 features will be surrounded in comments.
     * @param abbreviate
     *            if true, the package name will be removed from the annotations.
     *            The package name can be retrieved again by calling the
     *            {@link #getPackageNames()} method.
     * @return the text to insert
     */
    protected abstract String getText(boolean comments, boolean abbreviate);

    /**
     * Indicates if a preceding space should be added to this insertion.
     * Subclasses may override this method for custom leading space rules.
     *
     * @param gotSeparateLine
     *            {@code true} if this insertion is actually added on a separate
     *            line.
     * @param pos
     *            the source position where this insertion will be inserted.
     * @param precedingChar
     *            the character directly preceding where this insertion will be
     *            inserted. This value will be ignored if {@code pos} is 0.
     * @return {@code true} if a leading space should be added, {@code false}
     *         otherwise.
     */
    protected boolean addLeadingSpace(boolean gotSeparateLine, int pos,
            char precedingChar) {
        // Don't add a preceding space if this insertion is on its own line,
        // it's at the beginning of the file, the preceding character is already
        // whitespace, or the preceding character is the first formal or generic
        // parameter.
        return !gotSeparateLine && pos != 0
                && !Character.isWhitespace(precedingChar)
                && precedingChar != '(' && precedingChar != '<';
    }

    /**
     * Indicates if a trailing space should be added to this insertion.
     * Subclasses may override this method for custom trailing space rules.
     *
     * @param gotSeparateLine
     *            {@code true} if this insertion is actually added on a separate
     *            line.
     * @return {@code} true if a trailing space should be added, {@code false}
     * otherwise.
     */
    protected boolean addTrailingSpace(boolean gotSeparateLine) {
        // Don't added a trailing space if this insertion is on its own line.
        return !gotSeparateLine;
    }

    /**
     * Gets the package name.
     *
     * @return The package name of the annotation being inserted by this
     *         Insertion. This will be empty unless
     *         {@link #getText(boolean, boolean)} is called with abbreviate true.
     */
    public Set<String> getPackageNames() {
        return packageNames;
    }

    /**
     * Gets whether the insertion goes on a separate line.
     *
     * @return whether the insertion goes on a separate line
     */
    public boolean getSeparateLine() {
        return separateLine;
    }

    /**
     * Gets whether this insertion has already been inserted into source code.
     * See {@link ReceiverInsertion} for a better description of how this is
     * used.
     * @return {@code true} if this insertion has already been inserted,
     *         {@code false} otherwise.
     */
    public boolean getInserted() {
        return inserted;
    }

    /**
     * Sets whether this insertion has already been inserted into source code.
     * See {@link ReceiverInsertion} for a better description of how this is
     * used.
     * @param inserted {@code true} if this insertion has already been inserted,
     *         {@code false} otherwise.
     */
    public void setInserted(boolean inserted) {
        this.inserted = inserted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("(nl=%b) @ %s", separateLine, criteria);
    }

    /**
     * Gets the kind of this insertion.
     */
    public abstract Kind getKind();

    /**
     * Removes the leading package.
     *
     * @return given <code>@com.foo.bar(baz)</code> it returns the pair
     *         <code>{ com.foo, @bar(baz) }</code>.
     */
    public static Pair<String, String> removePackage(String s) {
        int nameEnd = s.indexOf("(");
        if (nameEnd == -1) {
            nameEnd = s.length();
        }
        int dotIndex = s.lastIndexOf(".", nameEnd);
        if (dotIndex != -1) {
            String packageName = s.substring(0, nameEnd);
            if (packageName.startsWith("@")) {
                return Pair.of(packageName.substring(1),
                        "@" + s.substring(dotIndex + 1));
            } else {
                return Pair.of(packageName, s.substring(dotIndex + 1));
            }
        } else {
            return Pair.of((String) null, s);
        }
    }

    /**
     * Converts the given type to a String. This method can't be in the
     * {@link Type} class because this method relies on the {@link Insertion}
     * class to format annotations, and the {@link Insertion} class is not
     * available from {@link Type}.
     *
     * @param type
     *            the type to convert
     * @param comments
     *            if true, Java 8 features will be surrounded in comments.
     * @param abbreviate
     *            if true, the package name will be removed from the annotations.
     *            The package name can be retrieved again by calling the
     *            {@link #getPackageNames()} method.
     * @return the type as a string
     */
    public String typeToString(Type type, boolean comments, boolean abbreviate) {
        StringBuilder result = new StringBuilder();

        switch (type.getKind()) {
        case DECLARED:
            DeclaredType declaredType = (DeclaredType) type;
            String typeName = declaredType.getName();
            int sep = typeName.lastIndexOf('.') + 1;
            if (abbreviate) {
                typeName = typeName.substring(sep);
            } else if (sep > 0) {
                result.append(typeName.substring(0, sep));
                typeName = typeName.substring(sep);
            }
            writeAnnotations(type, result, comments, abbreviate);
            result.append(typeName);
            if (!declaredType.isWildcard()) {
                List<Type> typeArguments = declaredType.getTypeParameters();
                if (!typeArguments.isEmpty()) {
                    result.append('<');
                    result.append(typeToString(typeArguments.get(0), comments, abbreviate));
                    for (int i = 1; i < typeArguments.size(); i++) {
                        result.append(", ");
                        result.append(typeToString(typeArguments.get(i), comments, abbreviate));
                    }
                    result.append('>');
                }
                Type innerType = declaredType.getInnerType();
                if (innerType != null) {
                    result.append('.');
                    result.append(typeToString(innerType, comments, abbreviate));
                }
            }
            break;
        case ARRAY:
            ArrayType arrayType = (ArrayType) type;
            result.append(typeToString(arrayType.getComponentType(), comments, abbreviate));
            if (!arrayType.getAnnotations().isEmpty()) {
                result.append(' ');
            }
            writeAnnotations(type, result, comments, abbreviate);
            result.append("[]");
            break;
        case BOUNDED:
            BoundedType boundedType = (BoundedType) type;
            result.append(typeToString(boundedType.getType(), comments, abbreviate));
            result.append(' ');
            result.append(boundedType.getBoundKind());
            result.append(' ');
            result.append(typeToString(boundedType.getBound(), comments, abbreviate));
            break;
        default:
            throw new RuntimeException("Illegal kind: " + type.getKind());
        }
        // There will be extra whitespace at the end if this is only annotations, so trim
        return result.toString().trim();
    }

    /**
     * Writes the annotations on the given type to the given
     * {@link StringBuilder}.
     *
     * @param type
     *            contains the annotations to write. Only the annotations
     *            directly on the type will be written. Subtypes will be
     *            ignored.
     * @param result
     *            where to write the annotations.
     * @param comments
     *            if {@code true}, Java 8 features will be surrounded in
     *            comments.
     * @param abbreviate
     *            if {@code true}, the package name will be removed from the
     *            annotations. The package name can be retrieved again by
     *            calling the {@link #getPackageNames()} method.
     */
    private void writeAnnotations(Type type, StringBuilder result,
            boolean comments, boolean abbreviate) {
        for (String annotation : type.getAnnotations()) {
            AnnotationInsertion ins = new AnnotationInsertion(annotation);
            result.append(ins.getText(comments, abbreviate));
            result.append(" ");
            if (abbreviate) {
                packageNames.addAll(ins.getPackageNames());
            }
        }
    }

    /**
     * Adds each of the given inner type insertions to the correct part of the
     * type, based on the insertion's type path.
     *
     * @param innerTypeInsertions
     *          The insertions to add to the type. These must be inner type
     *          insertions, meaning each of the insertions' {@link Criteria}
     *          must contain a {@link GenericArrayLocationCriterion} and
     *          {@link GenericArrayLocationCriterion#getLocation()} must return a
     *          non-empty list.
     * @param outerType The type to add the insertions to.
     */
    public static void decorateType(List<Insertion> innerTypeInsertions, Type outerType) {
        for (Insertion innerInsertion : innerTypeInsertions) {
            // Set each annotation as inserted (even if it doesn't actually get
            // inserted because of an error) to "disable" the insertion in the global
            // insertion list.
            innerInsertion.setInserted(true);

            try {
                if (innerInsertion.getKind() != Insertion.Kind.ANNOTATION) {
                    throw new RuntimeException("Expected 'ANNOTATION' insertion kind, got '"
                            + innerInsertion.getKind() + "'.");
                }
                GenericArrayLocationCriterion c = innerInsertion.getCriteria().getGenericArrayLocation();
                if (c == null) {
                    throw new RuntimeException("Missing type path.");
                }

                List<TypePathEntry> location = c.getLocation();
                Type type = outerType;

                // Use the type path entries to traverse through the type. Throw an
                // exception and move on to the next inner type insertion if the type
                // path and actual type don't match up.
                for (TypePathEntry tpe : location) {
                      switch (tpe.tag) {
                      case ARRAY:
                          if (type.getKind() == Type.Kind.ARRAY) {
                              type = ((ArrayType) type).getComponentType();
                          } else {
                              throw new RuntimeException("Incorrect type path.");
                          }
                          break;
                    case INNER_TYPE:
                          if (type.getKind() == Type.Kind.DECLARED) {
                            DeclaredType declaredType = (DeclaredType) type;
                            if (declaredType.getInnerType() == null) {
                              throw new RuntimeException("Incorrect type path: "
                                      + "expected inner type but none exists.");
                            }
                            type = declaredType.getInnerType();
                        } else {
                            throw new RuntimeException("Incorrect type path.");
                        }
                        break;
                    case WILDCARD:
                        if (type.getKind() == Type.Kind.BOUNDED) {
                            BoundedType boundedType = (BoundedType) type;
                            if (boundedType.getBound() == null) {
                              throw new RuntimeException("Incorrect type path: "
                                      + "expected type bound but none exists.");
                            }
                            type = boundedType.getBound();
                        } else {
                            throw new RuntimeException("Incorrect type path.");
                        }
                        break;
                    case TYPE_ARGUMENT:
                        if (type.getKind() == Type.Kind.DECLARED) {
                            DeclaredType declaredType = (DeclaredType) type;
                            if (0 <= tpe.arg && tpe.arg < declaredType.getTypeParameters().size()) {
                                type = ((DeclaredType) type).getTypeParameter(tpe.arg);
                            } else {
                                throw new RuntimeException("Incorrect type argument index: " + tpe.arg);
                            }
                        } else {
                            throw new RuntimeException("Incorrect type path.");
                        }
                        break;
                    default:
                        throw new RuntimeException("Illegal TypePathEntryKind: " + tpe.tag);
                    }
                }
                if (type.getKind() == Type.Kind.BOUNDED) {
                    // Annotations aren't allowed directly on the BoundedType, see BoundedType
                    type = ((BoundedType) type).getType();
                }
                type.addAnnotation(((AnnotationInsertion) innerInsertion).getAnnotation());
            } catch (Throwable e) {
                TreeFinder.reportInsertionError(innerInsertion, e);
            }
        }
    }
}
