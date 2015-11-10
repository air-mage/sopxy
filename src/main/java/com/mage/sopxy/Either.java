package com.mage.sopxy;

public class Either<A, B>
{
    private final A left;

    private final B right;


    public static <A, B, L extends A> Either<A, B> left(L left)
    {
        return new Either<>(left, null);
    }


    public static <A, B, R extends B> Either<A, B> right(R right)
    {
        return new Either<>(null, right);
    }


    private Either(A left, B right)
    {
        super();
        this.left = left;
        this.right = right;
    }


    public A getLeft()
    {
        return left;
    }


    public B getRight()
    {
        return right;
    }


    public boolean isLeft()
    {
        return left != null;
    }


    public boolean isRight()
    {
        return right != null;
    }
}
