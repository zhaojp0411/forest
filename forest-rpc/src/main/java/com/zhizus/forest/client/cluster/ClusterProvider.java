package com.zhizus.forest.client.cluster;

import com.google.common.collect.Lists;
import com.zhizus.forest.Referer;
import com.zhizus.forest.client.cluster.ha.FailFastStrategy;
import com.zhizus.forest.client.cluster.lb.AbstractLoadBalance;
import com.zhizus.forest.client.cluster.lb.RandomLoadBalance;
import com.zhizus.forest.common.codec.Message;
import org.apache.curator.x.discovery.ServiceInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Dempe on 2016/12/7.
 */
public class ClusterProvider<T> {

    private IHaStrategy<T> haStrategy;

    private AbstractLoadBalance<T> loadBalance;


    private List<Referer<T>> refererList;

    private AtomicBoolean available = new AtomicBoolean(false);

    private Collection<ServiceInstance> serviceInstances;

    public ClusterProvider(Collection<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;

    }

    public void init() throws InterruptedException {
        available.set(true);
        refererList = Lists.newArrayList();
        haStrategy = new FailFastStrategy<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            refererList.add(new Referer<T>(serviceInstance));
        }
        loadBalance = new RandomLoadBalance<>();
        loadBalance.setRefererList(refererList);

    }

    public Object call(Message message) throws Exception {
        return haStrategy.call(message, loadBalance);
    }


    public synchronized void onRefresh(List<Referer<T>> refererList) {
        if (refererList == null || refererList.size() < 1) {
            return;
        }
        loadBalance.onFresh(refererList);
        List<Referer<T>> oldRefererList = this.refererList;
        this.refererList = refererList;

        if (oldRefererList == null || oldRefererList.isEmpty()) {
            return;
        }

        List<Referer<T>> delayDestroyReferers = new ArrayList<Referer<T>>();

        for (Referer<T> referer : oldRefererList) {
            if (refererList.contains(referer)) {
                continue;
            }
            delayDestroyReferers.add(referer);
        }

    }
}
