/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

public class DataTreeCandidatePayloadTest {
    private DataTreeCandidate candidate;

    private static DataTreeCandidateNode findNode(final Collection<DataTreeCandidateNode> nodes, final PathArgument arg) {
        for (DataTreeCandidateNode node : nodes) {
            if (arg.equals(node.getIdentifier())) {
                return node;
            }
        }
        return null;
    }

    private static void assertChildrenEquals(final Collection<DataTreeCandidateNode> expected,
            final Collection<DataTreeCandidateNode> actual) {
        // Make sure all expected nodes are there
        for (DataTreeCandidateNode exp : expected) {
            final DataTreeCandidateNode act = findNode(actual, exp.getIdentifier());
            assertNotNull("missing expected child", act);
            assertCandidateNodeEquals(exp, act);
        }
        // Make sure no nodes are present which are not in the expected set
        for (DataTreeCandidateNode act : actual) {
            final DataTreeCandidateNode exp = findNode(expected, act.getIdentifier());
            assertNull("unexpected child", exp);
        }
    }

    private static void assertCandidateEquals(final DataTreeCandidate expected, final DataTreeCandidate actual) {
        assertEquals("root path", expected.getRootPath(), actual.getRootPath());

        final DataTreeCandidateNode expRoot = expected.getRootNode();
        final DataTreeCandidateNode actRoot = expected.getRootNode();
        assertEquals("root type", expRoot.getModificationType(), actRoot.getModificationType());

        switch (actRoot.getModificationType()) {
        case DELETE:
        case WRITE:
            assertEquals("root data", expRoot.getDataAfter(), actRoot.getDataAfter());
            break;
        case SUBTREE_MODIFIED:
            assertChildrenEquals(expRoot.getChildNodes(), actRoot.getChildNodes());
            break;
        default:
            fail("Unexpect root type " + actRoot.getModificationType());
            break;
        }

        assertCandidateNodeEquals(expected.getRootNode(), actual.getRootNode());
    }

    private static void assertCandidateNodeEquals(final DataTreeCandidateNode expected, final DataTreeCandidateNode actual) {
        assertEquals("child type", expected.getModificationType(), actual.getModificationType());
        assertEquals("child identifier", expected.getIdentifier(), actual.getIdentifier());

        switch (actual.getModificationType()) {
        case DELETE:
        case WRITE:
            assertEquals("child data", expected.getDataAfter(), actual.getDataAfter());
            break;
        case SUBTREE_MODIFIED:
            assertChildrenEquals(expected.getChildNodes(), actual.getChildNodes());
            break;
        default:
            fail("Unexpect root type " + actual.getModificationType());
            break;
        }
    }

    @Before
    public void setUp() {
        final YangInstanceIdentifier writePath = TestModel.TEST_PATH;
        final NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();
        candidate = DataTreeCandidates.fromNormalizedNode(writePath, writeData);
    }

    @Test
    public void testCandidateSerialization() throws IOException {
        final DataTreeCandidatePayload payload = DataTreeCandidatePayload.create(candidate);
        assertEquals("payload size", 141, payload.size());
    }

    @Test
    public void testCandidateSerDes() throws IOException {
        final DataTreeCandidatePayload payload = DataTreeCandidatePayload.create(candidate);
        assertCandidateEquals(candidate, payload.getCandidate());
    }

    @Test
    public void testPayloadSerDes() throws IOException {
        final DataTreeCandidatePayload payload = DataTreeCandidatePayload.create(candidate);
        assertCandidateEquals(candidate, SerializationUtils.clone(payload).getCandidate());
    }
}
