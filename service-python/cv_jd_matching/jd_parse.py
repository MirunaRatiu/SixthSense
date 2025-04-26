import re
import json
from typing import List, Dict, Any, Union

# --- Helper Functions (Mostly unchanged, slight refinements maybe) ---

def normalize_quotes(s: str) -> str:
    """Replaces various quote types with standard ones."""
    return s.replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"')

def find_closing_bracket(text: str, start_index: int, open_bracket: str = '{', close_bracket: str = '}') -> int:
    """Finds the index of the matching closing bracket, handling nesting."""
    balance = 1 # Start at 1 because we are already inside the opening bracket
    for i in range(start_index + 1, len(text)):
        if text[i] == open_bracket:
            balance += 1
        elif text[i] == close_bracket:
            balance -= 1
            if balance == 0:
                return i
    return -1 # Not found or mismatched

def parse_custom_value(value_str: str) -> Union[str, List[Any], Dict[str, Any]]:
    """Parses a value string, detecting if it's a list, object, or simple string."""
    value_str = value_str.strip()
    if not value_str:
        return "" # Handle empty values

    if value_str.startswith('[') and value_str.endswith(']'):
        # It's a list
        list_content = value_str[1:-1].strip()
        if not list_content:
            return []
        # Split list items carefully, respecting nested structures
        item_strings = []
        balance = 0
        last_split = 0
        for i, char in enumerate(list_content):
            if char == '{' or char == '[':
                balance += 1
            elif char == '}' or char == ']':
                balance -= 1
                if balance < 0: # Reset if malformed, though shouldn't happen ideally
                    balance = 0
            elif char == ',' and balance == 0:
                item_strings.append(list_content[last_split:i].strip())
                last_split = i + 1
        item_strings.append(list_content[last_split:].strip()) # Add the last item
        # Recursively parse each item
        parsed_items = []
        for item in item_strings:
            if item: # Avoid parsing empty strings
                try:
                    # Assume items in lists are blocks {} unless proven otherwise
                    if item.startswith('{') and item.endswith('}'):
                         parsed_items.append(parse_custom_block(item))
                    else:
                         # If not a block, treat as simple string? This might need adjustment
                         # based on expected list content. For group=[{...},{...}], items are blocks.
                         print(f"[WARN] Unexpected list item format (not a block {{...}}): '{item}'")
                         parsed_items.append({"raw_list_item": item}) # Or handle differently
                except Exception as e:
                     print(f"[ERROR] Failed parsing list item: '{item}'. Error: {e}")
                     parsed_items.append({"error": str(e), "raw": item})

        return parsed_items

    elif value_str.startswith('{') and value_str.endswith('}'):
        # It's a nested object
        try:
             return parse_custom_block(value_str)
        except Exception as e:
             print(f"[ERROR] Failed parsing nested block: '{value_str}'. Error: {e}")
             return {"error": str(e), "raw": value_str}
    else:
        # It's a simple string value (remove potential wrapping quotes ONLY if they wrap the entire string)
        if len(value_str) >= 2:
            if value_str.startswith('"') and value_str.endswith('"'):
                 return value_str[1:-1].replace('\\"', '"')
            if value_str.startswith("'") and value_str.endswith("'"):
                 return value_str[1:-1].replace("\\'", "'")
        return value_str # Return as is

# --- Core Parsing Logic (Revised) ---

def parse_custom_block(block_str: str) -> Dict[str, Any]:
    """Parses a single block string like '{key=value, group=[...]}' into a dictionary."""
    block_str = normalize_quotes(block_str.strip())
    if not block_str.startswith('{') or not block_str.endswith('}'):
        raise ValueError(f"Input string is not a valid block: '{block_str}'")

    content = block_str[1:-1].strip()
    parsed_data = {}
    current_pos = 0

    while current_pos < len(content):
        content_remaining = content[current_pos:]
        # Find the key
        match_key = re.match(r'\s*(\w+)\s*=\s*', content_remaining)
        if not match_key:
            if content_remaining.strip():
                 print(f"[DEBUG] No key found, remaining content: '{content_remaining}'")
            break # No more valid key= patterns

        key = match_key.group(1)
        start_value_pos_in_remaining = match_key.end()
        start_value_pos_in_content = current_pos + start_value_pos_in_remaining

        # Find the end of the value
        end_value_pos_in_content = -1
        value_start_char = content[start_value_pos_in_content] if start_value_pos_in_content < len(content) else None

        if value_start_char == '{':
            end_value_pos_in_content = find_closing_bracket(content, start_value_pos_in_content, '{', '}')
        elif value_start_char == '[':
            end_value_pos_in_content = find_closing_bracket(content, start_value_pos_in_content, '[', ']')
        else:
            # Simple string value. Find where the *next* key starts.
            # Search for ' , key = ' pattern starting *after* the current value begins.
            # The search needs to happen on the part of the string *after* the '=' sign.
            search_area_start = start_value_pos_in_remaining
            search_area_string = content_remaining[search_area_start:]

            # Regex: comma, optional whitespace, word chars (key), optional whitespace, equals sign
            next_key_match = re.search(r'\s*,\s*(\w+)\s*=', search_area_string)

            if next_key_match:
                # End position is right before the comma that starts the next key= pair
                # The match index is relative to 'search_area_string', need to adjust back to 'content'
                # end = start_in_content + start_in_remaining + match.start() - 1 (relative to comma)
                end_value_pos_in_content = start_value_pos_in_content + next_key_match.start() -1
            else:
                # No next key found, value extends to the end of the content
                end_value_pos_in_content = len(content) - 1

        if end_value_pos_in_content == -1 or end_value_pos_in_content < start_value_pos_in_content:
             raise ValueError(f"Could not determine end of value for key '{key}' starting at pos {start_value_pos_in_content} in block content: '{content}'")

        value_str = content[start_value_pos_in_content : end_value_pos_in_content + 1].strip()
        parsed_data[key] = parse_custom_value(value_str)

        # Update position for the next iteration: move past the parsed value
        current_pos = end_value_pos_in_content + 1
        # Skip the comma *if* it's immediately after the value and before the next key
        if current_pos < len(content) and content[current_pos] == ',':
             # Peek ahead to ensure we are skipping a separator comma, not one inside a value
             if re.match(r'\s*(\w+)\s*=', content[current_pos+1:]):
                 current_pos += 1 # Skip the separator comma

    return parsed_data

def parse_structured_string(raw: str) -> List[Dict[str, Any]]:
    """Parses the entire raw string containing multiple blocks."""
    if not raw:
        return []
    raw = normalize_quotes(raw.strip())
    blocks = []
    current_index = 0
    while current_index < len(raw):
        start_brace = raw.find('{', current_index)
        if start_brace == -1:
            break # No more blocks

        # Use find_closing_bracket starting from *inside* the brace
        end_brace = find_closing_bracket(raw, start_brace, '{', '}')
        if end_brace == -1:
            print(f"[WARN] Mismatched opening brace at index {start_brace} or unterminated block. Stopping parse for this field.")
            break # Stop parsing this field

        block_str = raw[start_brace : end_brace + 1]
        try:
            parsed_block = parse_custom_block(block_str)
            if parsed_block:
                blocks.append(parsed_block)
        except Exception as e:
            print(f"[PARSE ERROR] Failed on block:\n{block_str}\n----> Error: {e}\n")
            # Decide whether to raise e or continue
        finally:
             # Ensure progress even if parsing fails for one block
             current_index = end_brace + 1

    return blocks

# --- Transformation Logic (Mostly unchanged) ---

def _transform_parsed_node(node_dict: Dict[str, Any], kind: str) -> Dict[str, Any]:
    """Recursive helper to transform a parsed node into the target structure."""
    if not isinstance(node_dict, dict):
         # Handle cases where parse_custom_value might return non-dict (e.g., raw_list_item)
         if "raw_list_item" in node_dict: return {kind: node_dict["raw_list_item"]} # Or handle appropriately
         if "error" in node_dict: return {} # Skip nodes with errors
         print(f"[WARN] Internal: Expected dict node, got {type(node_dict)}: {node_dict}. Skipping.")
         return {}

    # Check if the node itself IS the leaf (e.g. directly {task=...} inside a group list)
    if kind in node_dict and "group" not in node_dict:
         return {kind: node_dict[kind]}
    # Check alternatives if primary 'kind' not present
    for potential_key in ["task", "requirement", "skill", "benefit"]:
         if potential_key in node_dict and "group" not in node_dict:
              return {potential_key: node_dict[potential_key]}


    # Check if it's an intermediate group node
    if "group" in node_dict:
        transformed_group = []
        group_val = node_dict.get("group") # Use .get for safety
        if isinstance(group_val, list):
             transformed_group = [_transform_parsed_node(child, kind) for child in group_val]
        elif isinstance(group_val, dict): # Handle case where list parsing failed but got single dict
             print(f"[WARN] Internal: 'group' key value is a dict, not list. Processing as single item: {group_val}")
             transformed_group = [_transform_parsed_node(group_val, kind)]
        else:
             print(f"[WARN] Internal: 'group' key found but value is not a list or dict: {group_val}")

        # Filter out empty results from recursive calls (e.g., from errors)
        filtered_group = [item for item in transformed_group if item]
        # Only return group structure if the group is not empty after filtering
        if filtered_group:
            return {
                "group": filtered_group,
                "group_type": node_dict.get("group_type", "AND")
            }
        else:
             # If group becomes empty after filtering, return empty dict to avoid adding empty groups
             return {}
    else:
        # If it's not a group and not a direct leaf node (checked above),
        # it might be a block with only 'original_statement' or an error case.
        # The calling function adds original_statement.
        # Suppress warning if only 'original_statement' exists.
        expected_keys = ["task", "requirement", "skill", "benefit", "group", "group_type", "original_statement", "error", "raw"]
        is_only_original = all(k == "original_statement" for k in node_dict.keys())
        has_unexpected_keys = any(k not in expected_keys for k in node_dict.keys())

        if not is_only_original and has_unexpected_keys:
             # This warning helps catch parsing issues where unexpected keys remain
             print(f"[DEBUG] Node has unexpected structure or missing leaf key ('{kind}'): {node_dict}")
        return {} # Return empty dict as the leaf/group data is handled elsewhere/missing


def process_field(parsed_data: List[Dict[str, Any]], kind: str) -> List[Dict[str, Any]]:
    """Processes the list of parsed dictionaries for a specific field."""
    results = []
    for item_dict in parsed_data:
        if not isinstance(item_dict, dict):
             print(f"[WARN] Skipping non-dict item in parsed_data: {item_dict}")
             continue
        if "error" in item_dict: # Skip items where parsing failed severely
             print(f"[WARN] Skipping item due to parsing error: {item_dict}")
             continue

        # Always include original_statement if present in the parsed block
        output = {}
        if "original_statement" in item_dict:
             output["original_statement"] = item_dict.get("original_statement", "")

        # Transform the rest of the node (group structure or leaf key like task/req)
        transformed_node_content = _transform_parsed_node(item_dict, kind)

        # Merge the transformed content (group/group_type or task/req/...)
        output.update(transformed_node_content)

        # Final check: If it's NOT a group AND the specific 'kind' key is missing, AND original_statement is present,
        # add the empty 'kind' key. This handles blocks like {original_statement=..., requirement=...} correctly
        # and adds empty kind for {original_statement=...} only blocks.
        if "group" not in output and kind not in output:
            # Check if *any* other known leaf key exists before adding empty 'kind'
            has_any_leaf_key = any(k in output for k in ["task", "requirement", "skill", "benefit"])
            if not has_any_leaf_key and "original_statement" in output:
                 # Only add empty 'kind' if it's essentially just original_statement
                 output[kind] = ""

        # Only add to results if the output is not empty (e.g. skip fully failed parses)
        # Always add if original_statement is present.
        if output and "original_statement" in output:
             results.append(output)
        elif not output:
             print(f"[DEBUG] Skipping empty output generated from item: {item_dict}")


    return results


def transform_dto_to_jd(dto: Dict[str, Any]) -> Dict[str, Any]:
    """Transforms the raw DTO into the structured format using the refined parser."""

    key_resp_raw = dto.get("keyResponsibilities", "")
    req_qual_raw = dto.get("requiredQualifications", "")
    pref_skills_raw = dto.get("preferredSkills", "")
    benefits_raw = dto.get("benefits", "") # Handle benefits if present

    job_title = dto.get("jobTitle", "Job")
    message = f"{job_title} related industry context" # Example message

    return {
        "job_title": dto.get("jobTitle"),
        "company_overview": dto.get("companyOverview"),
        "message": message,
        "key_responsibilities": process_field(
            parse_structured_string(key_resp_raw), "task"
        ),
        "required_qualifications": process_field(
            parse_structured_string(req_qual_raw), "requirement"
        ),
        "preferred_skills": process_field(
            parse_structured_string(pref_skills_raw), "skill"
        ),
        "benefits": process_field(
            parse_structured_string(benefits_raw), "benefit"
        )
    }
#
# # --- Input Data (Exact copy as provided by user) ---
# job_dto_raw = {
#     "id": 101,
#     "jobTitle": "Senior UI/UX Designer",
#     "companyOverview": "InnovateTech Solutions is a leading technology company dedicated to creating cutting-edge digital products that enhance user experiences across various platforms. Our team is passionate about innovation, creativity, and delivering exceptional solutions that meet the evolving needs of our clients. We pride ourselves on fostering a collaborative and inclusive work environment where every team member's ideas are valued and contribute to our success.",
#     "keyResponsibilities": "{original_statement=Lead the design and development of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey., group=[{group=[{task=Lead the design of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey}, {task=Lead the development of user interfaces and experiences for web and mobile applications, ensuring a seamless and intuitive user journey}], group_type=AND}], group_type=AND} {original_statement=Collaborate with cross-functional teams, including product managers, developers, and other designers, to translate business requirements into innovative design solutions., task=Collaborate with cross-functional teams, including product managers, developers, and other designers, to translate business requirements into innovative design solutions} {original_statement=Conduct user research and usability testing to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction., group=[{group=[{task=Conduct user research to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction}, {task=Conduct usability testing to gather insights and validate design concepts, iterating based on feedback to enhance user satisfaction}], group_type=AND}], group_type=AND} {original_statement=Create wireframes, prototypes, and high-fidelity designs using industry-standard design tools, ensuring consistency with brand guidelines and design systems., group=[{group=[{task=Create wireframes using industry-standard design tools, ensuring consistency with brand guidelines and design systems}, {task=Create prototypes using industry-standard design tools, ensuring consistency with brand guidelines and design systems}, {task=Create high-fidelity designs using industry-standard design tools, ensuring consistency with brand guidelines and design systems}], group_type=AND}], group_type=AND} {original_statement=Mentor and provide guidance to junior designers, fostering a culture of continuous learning and improvement within the design team., group=[{group=[{task=Mentor junior designers, fostering a culture of continuous learning and improvement within the design team}, {task=Provide guidance to junior designers, fostering a culture of continuous learning and improvement within the design team}], group_type=AND}], group_type=AND} {original_statement=Stay updated with the latest UI/UX trends, techniques, and technologies, and apply them to improve design processes and deliverables., task=Stay updated with the latest UI/UX trends, techniques, and technologies, and apply them to improve design processes and deliverables} {original_statement=Present design concepts and solutions to stakeholders, articulating design rationale and incorporating feedback to refine designs., task=Present design concepts and solutions to stakeholders, articulating design rationale and incorporating feedback to refine designs} ",
#     "requiredQualifications": "{original_statement=Bachelor’s degree in Design, Human-Computer Interaction, or a related field., group=[{group=[{requirement=Bachelor’s degree in Design}, {requirement=Bachelor’s degree in Human-Computer Interaction}, {requirement=Bachelor’s degree in a related field}], group_type=OR}], group_type=AND} {original_statement=Minimum of 5 years of experience in UI/UX design, with a strong portfolio showcasing diverse design projects., requirement=Minimum of 5 years of experience in UI/UX design, with a strong portfolio showcasing diverse design projects} {original_statement=Proficiency in design software such as Adobe Creative Suite, Sketch, Figma, or similar tools., group=[{group=[{requirement=Proficiency in design software such as Adobe Creative Suite}, {requirement=Proficiency in design software such as Sketch}, {requirement=Proficiency in design software such as Figma}, {requirement=Proficiency in design software such as similar tools}], group_type=OR}], group_type=AND} {original_statement=Strong understanding of user-centered design principles and best practices., requirement=Strong understanding of user-centered design principles and best practices} {original_statement=Excellent communication and presentation skills, with the ability to articulate design decisions effectively., group=[{group=[{requirement=Excellent communication skills, with the ability to articulate design decisions effectively}, {requirement=Excellent presentation skills, with the ability to articulate design decisions effectively}], group_type=AND}], group_type=AND}",
#     "preferredSkills": "{original_statement=Experience with front-end development technologies such as HTML, CSS, and JavaScript., group=[{group=[{skill=Experience with front-end development technologies such as HTML}, {skill=Experience with front-end development technologies such as CSS}, {skill=Experience with front-end development technologies such as JavaScript}], group_type=AND}], group_type=AND} {original_statement=Familiarity with agile methodologies and working in an agile environment., group=[{group=[{skill=Familiarity with agile methodologies}, {skill=Working in an agile environment}], group_type=AND}], group_type=AND} {original_statement=Knowledge of accessibility standards and best practices in design., skill=Knowledge of accessibility standards and best practices in design} {original_statement=Experience in designing for a variety of platforms, including web, mobile, and emerging technologies like AR/VR., group=[{group=[{skill=Experience in designing for a variety of platforms, including web}, {skill=Experience in designing for a variety of platforms, including mobile}, {skill=Experience in designing for a variety of platforms, including emerging technologies like AR/VR}], group_type=AND}], group_type=AND}"
# }
#
# # --- Transformation ---
# transformed_dto = transform_dto_to_jd(job_dto_raw)
#
# # --- Output ---
# print(json.dumps(transformed_dto, indent=2))