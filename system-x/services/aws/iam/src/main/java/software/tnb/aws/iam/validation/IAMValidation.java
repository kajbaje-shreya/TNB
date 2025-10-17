package software.tnb.aws.iam.validation;

import software.tnb.common.validation.Validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachedPolicy;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;

public class IAMValidation implements Validation {
    private static final Logger LOG = LoggerFactory.getLogger(IAMValidation.class);

    private final IamClient client;

    public IAMValidation(IamClient client) {
        this.client = client;
    }

    public String createRole(String name, String description, String rolePolicyDocument) {
        if (roleExists(name)) {
            LOG.debug("Role {} already exists, skipping creation.", name);
            return getRoleArn(name).get();
        } else {
            LOG.debug("Creating IAM role {}", name);
            final String arn = client.createRole(b -> b.roleName(name)
                .description(description)
                .assumeRolePolicyDocument(rolePolicyDocument)
            ).role().arn();

            client.waiter().waitUntilRoleExists(r -> r.roleName(name));

            return arn;
        }
    }

    public String createPolicy(String name, String policyDocument) {
        final String arn = client.createPolicy(b -> b.policyName(name)
            .policyDocument(policyDocument)
        ).policy().arn();

        client.waiter().waitUntilPolicyExists(b -> b.policyArn(arn));

        return arn;
    }

    public void attachPolicy(String role, String policyArn) {
        final Optional<AttachedPolicy> policy = client.listAttachedRolePolicies(b -> b.roleName(role)).attachedPolicies().stream()
            .filter(p -> policyArn.equals(p.policyArn())).findFirst();
        if (policy.isEmpty()) {
            LOG.debug("Attaching policy {} to role {}", policyArn, role);
            client.attachRolePolicy(b -> b.roleName(role).policyArn(policyArn));
        }
    }

    public boolean roleExists(String name) {
        return getRoleArn(name).isPresent();
    }

    public Optional<String> getRoleArn(String name) {
        try {
            return Optional.of(client.getRole(b -> b.roleName(name)).role().arn());
        } catch (NoSuchEntityException e) {
            return Optional.empty();
        }
    }

    public void deleteRole(String name) {
        LOG.debug("Deleting role {}", name);
        client.deleteRole(b -> b.roleName(name));
    }
}
