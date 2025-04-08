# How to create a csv file containing all currently available APIs

This file also contains an ACTIONS column with possible changes that need to be made to the API to improve it.

1. Ensure `hmpps-approved-premises-api` is running locally (use `hmpps-approved-premises-tools` and follow the README).
2. In the root directory run the following:
  ```shell
  script/create_api_summary_csv
  ```
3. Open the created `api_summary.csv` file (in `build/tmp/`) in your desired editor.
