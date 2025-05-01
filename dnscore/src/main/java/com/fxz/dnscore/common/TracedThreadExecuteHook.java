package com.fxz.dnscore.common;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import com.fxz.component.fuled.cat.starter.util.CatPropertyContext;
import com.fxz.fuled.dynamic.threadpool.RpcContext;
import com.fxz.fuled.dynamic.threadpool.manage.impl.ThreadExecuteHookReporter;
import com.fxz.fuled.dynamic.threadpool.wrapper.TaskWrapper;

import java.util.Objects;

import static com.dianping.cat.Cat.*;

public class TracedThreadExecuteHook extends ThreadExecuteHookReporter {
    ThreadLocal<Transaction> transactionThreadLocal = new ThreadLocal<>();

    @Override
    public void onException(TaskWrapper taskWrapper, Throwable throwable) {
        super.onException(taskWrapper, throwable);
        Transaction transaction = transactionThreadLocal.get();
        Cat.logEvent("onException", taskWrapper.getThreadPoolName());
        if (Objects.nonNull(transaction)) {
            transaction.setStatus(throwable);
            transaction.complete();
        }
    }

    @Override
    public void afterExecute(TaskWrapper taskWrapper) {
        super.afterExecute(taskWrapper);
        Transaction transaction = transactionThreadLocal.get();
        Cat.logEvent("afterExecute", taskWrapper.getThreadPoolName());
        if (Objects.nonNull(transaction)) {
            transaction.setStatus(Transaction.SUCCESS);
            transaction.complete();
        }

    }

    @Override
    public void enqueue(TaskWrapper taskWrapper) {
        super.enqueue(taskWrapper);
        Transaction t = Cat.newTransaction("CrossThreadPool", taskWrapper.getThreadPoolName());
        CatPropertyContext context = new CatPropertyContext();
        Cat.logRemoteCallClient(context, Cat.getManager().getDomain());
        taskWrapper.setMeta(context);
        t.complete();

    }

    @Override
    public void beforeExecute(TaskWrapper taskWrapper) {
        super.beforeExecute(taskWrapper);
        CatPropertyContext catPropertyContext = (CatPropertyContext) RpcContext.get();
        getManager().setup();
        MessageTree tree = getManager().getThreadLocalMessageTree();
        String childId = catPropertyContext.getProperty(Context.CHILD);
        String rootId = catPropertyContext.getProperty(Context.ROOT);
        String parentId = catPropertyContext.getProperty(Context.PARENT);
        if (parentId != null) {
            tree.setParentMessageId(parentId);
        }
        if (rootId != null) {
            tree.setRootMessageId(rootId);
        }
        if (childId != null) {
            tree.setMessageId(childId);
        }
        Transaction transaction = newTransaction("Execute", taskWrapper.getThreadPoolName());
        Cat.logEvent("beforeExecute", taskWrapper.getThreadPoolName());
        transactionThreadLocal.set(transaction);
    }
}
