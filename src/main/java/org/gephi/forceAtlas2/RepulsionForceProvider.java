package org.gephi.forceAtlas2;


public interface RepulsionForceProvider {

    ForceFactory.RepulsionForce getRepulsionForce(boolean adjustBySize, double coefficent);
}
