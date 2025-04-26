package com.fxz.dnscore.common;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;
import com.fxz.component.fuled.cat.starter.util.CatPropertyContext;
import com.fxz.fuled.dynamic.threadpool.RpcContext;
import com.fxz.fuled.dynamic.threadpool.manage.impl.DefaultThreadExecuteHook;
import com.fxz.fuled.dynamic.threadpool.wrapper.TaskWrapper;

import static com.dianping.cat.Cat.*;

public class TracedThreadExecuteHook extends DefaultThreadExecuteHook {
    ThreadLocal<Transaction> transactionThreadLocal = new ThreadLocal<>();

    @Override
    public void onException(TaskWrapper taskWrapper, Throwable throwable) {
        super.onException(taskWrapper, throwable);
        Transaction transaction = transactionThreadLocal.get();
        transaction.setStatus(throwable);
        transaction.complete();
    }

    @Override
    public void afterExecute(TaskWrapper taskWrapper) {
        super.afterExecute(taskWrapper);
        Transaction transaction = transactionThreadLocal.get();
        transaction.setStatus(Transaction.SUCCESS);
        transaction.complete();

    }

    @Override
    public void enqueue(TaskWrapper taskWrapper) {
        super.enqueue(taskWrapper);
        Transaction t = Cat.newTransaction("CrossThreadPool", taskWrapper.getThreadPoolName());
        CatPropertyContext context = new CatPropertyContext();
//        MessageTree tree = getManager().getThreadLocalMessageTree();
//        String messageId = tree.getMessageId();
//        if (messageId == null) {
//            messageId = getProducer().createMessageId();
//            tree.setMessageId(messageId);
//        }
//        String childId = getProducer().createRpcServerId(Cat.getManager().getDomain());
//        logEvent("CrossThread", "", "0", childId);
//        String root = tree.getRootMessageId();
//        if (root == null) {
//            root = messageId;
//        }
//        context.addProperty(Context.ROOT, root);
//        context.addProperty(Context.PARENT, messageId);
//        context.addProperty(Context.CHILD, childId);
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
        Transaction beforeExecute_ = newTransaction("Execute", taskWrapper.getThreadPoolName());
        transactionThreadLocal.set(beforeExecute_);
    }
}
