package com.mage.sopxy.internal;

public class InternalException extends Exception
{
    private static final long serialVersionUID = 1L;

    public InternalException()
    {
        super();
    }

    public InternalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InternalException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InternalException(String message)
    {
        super(message);
    }

    public InternalException(Throwable cause)
    {
        super(cause);
    }
}
