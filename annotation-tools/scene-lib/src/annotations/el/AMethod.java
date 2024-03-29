package annotations.el;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.javari.qual.ReadOnly;
*/

import java.util.Map;

import annotations.util.coll.VivifyingMap;

/**
 * An annotated method; contains bounds, return, parameters, receiver, and throws.
 */
public final class AMethod extends ADeclaration {
    /** The method's annotated type parameter bounds */
    public final VivifyingMap<BoundLocation, ATypeElement> bounds =
            ATypeElement.<BoundLocation>newVivifyingLHMap_ATE();

    /** The method's annotated return type */
    public final ATypeElement returnType; // initialized in constructor

    /** The method's annotated receiver parameter type */
    public final AField receiver; // initialized in constructor

    /** The method's annotated parameters; map key is parameter index */
    public final VivifyingMap<Integer, AField> parameters =
            AField.<Integer>newVivifyingLHMap_AF();

    public final VivifyingMap<TypeIndexLocation, ATypeElement> throwsException =
        ATypeElement.<TypeIndexLocation>newVivifyingLHMap_ATE();

    public ABlock body;
    private final String methodName;

    AMethod(String methodName) {
      super("method: " + methodName);
      this.methodName = methodName;
      this.body = new ABlock(methodName);
      returnType = new ATypeElement("return type of " + methodName);
      receiver = new AField("receiver parameter type of " + methodName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(/*>>> @ReadOnly AMethod this, */ /*@ReadOnly*/ AElement o) {
        return o instanceof AMethod &&
            ((/*@ReadOnly*/ AMethod) o).equalsMethod(this);
    }

    boolean equalsMethod(/*>>> @ReadOnly AMethod this, */ /*@ReadOnly*/ AMethod o) {
        parameters.prune();
        o.parameters.prune();

        return super.equals(o)
            && returnType.equalsTypeElement(o.returnType)
            && bounds.equals(o.bounds)
            && receiver.equals(o.receiver)
            && parameters.equals(o.parameters)
            && body.equals(o.body)
            && throwsException.equals(o.throwsException);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(/*>>> @ReadOnly AMethod this*/) {
        return super.hashCode() + bounds.hashCode()
                + receiver.hashCode() + parameters.hashCode()
                + throwsException.hashCode() + body.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean prune() {
        return super.prune() & bounds.prune()
            & returnType.prune()
            & receiver.prune() & parameters.prune()
            & throwsException.prune() & body.prune();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AMethod ");
        sb.append(methodName);
        sb.append(": (");
        sb.append(" -1:");
        sb.append(receiver.toString());
        // int size = parameters.size();
        for (Map.Entry<Integer, AField> em : parameters.entrySet()) {
            Integer i = em.getKey();
            sb.append(" ");
            sb.append(i);
            sb.append(":");
            AElement ae = em.getValue();
            sb.append(ae.toString());
            sb.append(" ");
            ATypeElement ate = ae.type;
            sb.append(ate.toString());
        }
        sb.append(" ");
        sb.append("ret:");
        sb.append(returnType.toString());
        sb.append(") ");
        sb.append(body.toString());
        return sb.toString();
    }

    @Override
    public <R, T> R accept(ElementVisitor<R, T> v, T t) {
        return v.visitMethod(this, t);
    }
}
